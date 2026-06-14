package com.brainsmooch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.brainsmooch.MainActivity
import com.brainsmooch.R
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BlockerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Written on the main thread (onStartCommand), read from the IO packet loop.
    @Volatile
    private var blockedDomains: Set<String> = emptySet()

    private var soundPool: SoundPool? = null
    private var bellSoundId = 0
    private val lastBellMillis = ConcurrentHashMap<String, Long>()

    companion object {
        const val CHANNEL_ID = "brain_smooch_vpn"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.brainsmooch.START_VPN"
        const val ACTION_STOP = "com.brainsmooch.STOP_VPN"
        const val ACTION_UPDATE_DOMAINS = "com.brainsmooch.UPDATE_DOMAINS"
        const val EXTRA_DOMAINS = "domains"
        const val EXTRA_END_TIME = "end_time"

        private const val VPN_ADDRESS = "10.0.0.1"
        private const val VPN_DNS = "10.0.0.2"
        private const val VPN_MTU = 1500
        private const val UPSTREAM_DNS = "8.8.8.8"
        private const val DNS_PORT = 53
        private const val PROTOCOL_UDP = 17
        private const val UDP_HEADER_LENGTH = 8
        private const val MAX_PACKET_SIZE = 32767
        private const val DNS_RESPONSE_BUFFER_SIZE = 512
        private const val DNS_SOCKET_TIMEOUT_MS = 5_000
        private const val BELL_THROTTLE_MS = 4_000L

        /**
         * Always blocked to stop DNS-over-HTTPS bypass. Kept separate from the
         * user's list so the bell never rings for their constant background noise.
         */
        private val DOH_PROVIDERS = setOf(
            "dns.google",
            "dns.google.com",
            "dns64.dns.google",
            "cloudflare-dns.com",
            "one.one.one.one",
            "1dot1dot1dot1.cloudflare-dns.com",
            "dns.cloudflare.com",
            "mozilla.cloudflare-dns.com",
            "doh.opendns.com",
            "dns.quad9.net",
            "dns9.quad9.net",
            "dns.nextdns.io",
            "doh.cleanbrowsing.org",
            "dns.adguard.com"
        )

        private val RELATED_DOMAINS = mapOf(
            "youtube.com" to listOf("youtu.be", "googlevideo.com", "ytimg.com", "yt.be"),
            "twitter.com" to listOf("x.com", "twimg.com", "t.co"),
            "facebook.com" to listOf("fb.com", "fbcdn.net", "facebook.net"),
            "instagram.com" to listOf("i.instagram.com", "graph.instagram.com", "b.i.instagram.com", "cdninstagram.com", "ig.me"),
            "tiktok.com" to listOf("tiktokcdn.com", "musical.ly"),
            "reddit.com" to listOf("redd.it", "redditmedia.com", "redditstatic.com"),
            "snapchat.com" to listOf("snap.com", "sc-cdn.net"),
            "linkedin.com" to listOf("licdn.com"),
            "pinterest.com" to listOf("pinimg.com")
        )

        fun start(context: Context, domains: List<String>, endTimeMillis: Long) {
            val intent = Intent(context, BlockerVpnService::class.java).apply {
                action = ACTION_START
                putStringArrayListExtra(EXTRA_DOMAINS, ArrayList(domains))
                putExtra(EXTRA_END_TIME, endTimeMillis)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BlockerVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /** Swap in a new domain list without tearing down the running tunnel. */
        fun updateDomains(context: Context, domains: List<String>) {
            val intent = Intent(context, BlockerVpnService::class.java).apply {
                action = ACTION_UPDATE_DOMAINS
                putStringArrayListExtra(EXTRA_DOMAINS, ArrayList(domains))
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            .build()
            .also { bellSoundId = it.load(this, R.raw.boom, 1) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val domains = intent.getStringArrayListExtra(EXTRA_DOMAINS) ?: emptyList()
                val endTime = intent.getLongExtra(EXTRA_END_TIME, 0L)
                blockedDomains = expandDomains(domains.map { normalizeDomain(it) })
                startVpn(endTime)
            }
            ACTION_UPDATE_DOMAINS -> {
                val domains = intent.getStringArrayListExtra(EXTRA_DOMAINS) ?: emptyList()
                blockedDomains = expandDomains(domains.map { normalizeDomain(it) })
            }
            ACTION_STOP -> {
                stopVpn()
            }
        }
        return START_STICKY
    }

    private fun startVpn(endTimeMillis: Long) {
        if (isRunning) return
        isRunning = true

        startForeground(NOTIFICATION_ID, createNotification(endTimeMillis))

        // Route ONLY the fake DNS server into the tunnel. Routing 0.0.0.0/0 here
        // would pull every packet on the phone into this loop, which only knows
        // how to answer DNS — killing all other traffic for every site.
        val builder = Builder()
            .setSession("BrainSmooch")
            .addAddress(VPN_ADDRESS, 24)
            .addRoute(VPN_DNS, 32)
            .addDnsServer(VPN_DNS)
            .setMtu(VPN_MTU)
            .setBlocking(false)

        vpnInterface = builder.establish()

        vpnInterface?.let { pfd ->
            scope.launch { handleDnsRequests(pfd) }
        }
    }

    private fun stopVpn() {
        isRunning = false
        scope.cancel()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun handleDnsRequests(pfd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteArray(MAX_PACKET_SIZE)

        while (isRunning) {
            try {
                val length = inputStream.read(packet)
                if (length <= 0) continue

                val buffer = ByteBuffer.wrap(packet, 0, length)
                val ipVersion = (buffer.get(0).toInt() and 0xF0) shr 4

                if (ipVersion == 4) {
                    val protocol = buffer.get(9).toInt() and 0xFF
                    if (protocol == PROTOCOL_UDP) {
                        val destPort = ((buffer.get(22).toInt() and 0xFF) shl 8) or (buffer.get(23).toInt() and 0xFF)
                        if (destPort == DNS_PORT) {
                            handleDnsPacket(buffer, length, outputStream)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isRunning) continue
                break
            }
        }
    }

    private fun handleDnsPacket(packet: ByteBuffer, length: Int, outputStream: FileOutputStream) {
        val ipHeaderLength = (packet.get(0).toInt() and 0x0F) * 4
        val dnsStart = ipHeaderLength + UDP_HEADER_LENGTH

        val domain = extractDomainFromDns(packet.array(), dnsStart, length)
        val matched = findBlockedEntry(domain)

        if (matched != null) {
            if (matched !in DOH_PROVIDERS) {
                playBellThrottled(matched)
            }
            val response = createBlockedDnsResponse(packet.array(), length, ipHeaderLength)
            outputStream.write(response)
        } else {
            forwardDnsQuery(packet.array(), length, outputStream, ipHeaderLength)
        }
    }

    private fun extractDomainFromDns(data: ByteArray, dnsStart: Int, length: Int): String {
        val domain = StringBuilder()
        var pos = dnsStart + 12 // Skip DNS header

        while (pos < length && data[pos].toInt() != 0) {
            val labelLength = data[pos].toInt() and 0xFF
            if (labelLength == 0) break
            if (domain.isNotEmpty()) domain.append('.')
            pos++
            repeat(labelLength) {
                if (pos < length) {
                    domain.append(data[pos].toInt().toChar())
                    pos++
                }
            }
        }
        return domain.toString().lowercase()
    }

    /** Returns the blocklist entry the queried domain matched, or null if allowed. */
    private fun findBlockedEntry(domain: String): String? {
        val normalized = normalizeDomain(domain)
        return blockedDomains.firstOrNull { blocked ->
            normalized == blocked || normalized.endsWith(".$blocked")
        }
    }

    /**
     * Ding when a blocked site is attempted. Keyed on the matched blocklist entry
     * so a burst of related lookups (youtube.com + googlevideo.com + …) rings once.
     */
    private fun playBellThrottled(matchedEntry: String) {
        val now = System.currentTimeMillis()
        val last = lastBellMillis[matchedEntry] ?: 0L
        if (now - last >= BELL_THROTTLE_MS) {
            lastBellMillis[matchedEntry] = now
            soundPool?.play(bellSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun normalizeDomain(domain: String): String {
        return domain.lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .trim()
            .trimEnd('/')
    }

    private fun expandDomains(domains: List<String>): Set<String> {
        val expanded = mutableSetOf<String>()
        expanded.addAll(DOH_PROVIDERS)

        for (domain in domains) {
            expanded.add(domain)
            RELATED_DOMAINS[domain]?.let { related ->
                expanded.addAll(related)
            }
        }
        return expanded
    }

    private fun createBlockedDnsResponse(query: ByteArray, length: Int, ipHeaderLength: Int): ByteArray {
        val dnsStart = ipHeaderLength + UDP_HEADER_LENGTH

        // Find the end of the question section: QNAME labels, then QTYPE+QCLASS.
        var pos = dnsStart + 12
        while (pos < length && query[pos].toInt() != 0) {
            pos += (query[pos].toInt() and 0xFF) + 1
        }
        val questionEnd = pos + 1 + 4
        if (questionEnd > length) return ByteArray(0) // malformed query, drop it

        val qType = ((query[pos + 1].toInt() and 0xFF) shl 8) or (query[pos + 2].toInt() and 0xFF)
        val isAQuery = qType == 1

        // Response = headers + question (EDNS extras stripped), plus a 16-byte
        // "0.0.0.0" answer for A queries; other types get an empty NOERROR.
        val totalLength = if (isAQuery) questionEnd + 16 else questionEnd
        val response = query.copyOf(totalLength)

        // Swap source and dest IP
        for (i in 0..3) {
            val temp = response[12 + i]
            response[12 + i] = response[16 + i]
            response[16 + i] = temp
        }

        // Swap source and dest port
        val srcPort = response[ipHeaderLength]
        val srcPort2 = response[ipHeaderLength + 1]
        response[ipHeaderLength] = response[ipHeaderLength + 2]
        response[ipHeaderLength + 1] = response[ipHeaderLength + 3]
        response[ipHeaderLength + 2] = srcPort
        response[ipHeaderLength + 3] = srcPort2

        // DNS header: response flags, ANCOUNT, and zeroed NSCOUNT/ARCOUNT
        response[dnsStart + 2] = 0x81.toByte()
        response[dnsStart + 3] = 0x80.toByte()
        response[dnsStart + 6] = 0x00
        response[dnsStart + 7] = if (isAQuery) 0x01 else 0x00
        response[dnsStart + 8] = 0x00
        response[dnsStart + 9] = 0x00
        response[dnsStart + 10] = 0x00
        response[dnsStart + 11] = 0x00

        if (isAQuery) {
            // Answer section (name pointer to the question, A record = 0.0.0.0)
            val answerStart = questionEnd
            response[answerStart] = 0xC0.toByte()
            response[answerStart + 1] = 0x0C.toByte()
            response[answerStart + 2] = 0x00
            response[answerStart + 3] = 0x01 // TYPE A
            response[answerStart + 4] = 0x00
            response[answerStart + 5] = 0x01 // CLASS IN
            response[answerStart + 6] = 0x00
            response[answerStart + 7] = 0x00
            response[answerStart + 8] = 0x00
            response[answerStart + 9] = 0x3C // TTL 60s
            response[answerStart + 10] = 0x00
            response[answerStart + 11] = 0x04
            response[answerStart + 12] = 0x00 // 0.0.0.0
            response[answerStart + 13] = 0x00
            response[answerStart + 14] = 0x00
            response[answerStart + 15] = 0x00
        }

        // Without corrected lengths and checksums the OS drops the packet.
        finalizeIpUdpHeaders(response, ipHeaderLength, totalLength)

        return response
    }

    /**
     * Stamps correct IP total length, UDP length, IP header checksum and a
     * zeroed UDP checksum (legal for UDP over IPv4) onto a crafted packet.
     */
    private fun finalizeIpUdpHeaders(packet: ByteArray, ipHeaderLength: Int, totalLength: Int) {
        packet[2] = ((totalLength shr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()

        val udpLength = totalLength - ipHeaderLength
        packet[ipHeaderLength + 4] = ((udpLength shr 8) and 0xFF).toByte()
        packet[ipHeaderLength + 5] = (udpLength and 0xFF).toByte()
        packet[ipHeaderLength + 6] = 0
        packet[ipHeaderLength + 7] = 0

        packet[10] = 0
        packet[11] = 0
        var sum = 0
        var i = 0
        while (i < ipHeaderLength) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = sum.inv() and 0xFFFF
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()
    }

    private fun forwardDnsQuery(packet: ByteArray, length: Int, outputStream: FileOutputStream, ipHeaderLength: Int) {
        scope.launch {
            try {
                val dnsStart = ipHeaderLength + UDP_HEADER_LENGTH
                val dnsLength = length - dnsStart
                val dnsQuery = packet.copyOfRange(dnsStart, length)

                val socket = DatagramSocket()
                protect(socket)
                socket.soTimeout = DNS_SOCKET_TIMEOUT_MS

                val dnsServer = InetAddress.getByName(UPSTREAM_DNS)
                val outPacket = DatagramPacket(dnsQuery, dnsLength, dnsServer, DNS_PORT)
                socket.send(outPacket)

                val responseBuffer = ByteArray(DNS_RESPONSE_BUFFER_SIZE)
                val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(inPacket)
                socket.close()

                val response = buildDnsResponsePacket(packet, ipHeaderLength, responseBuffer, inPacket.length)
                outputStream.write(response)
            } catch (e: Exception) {
                // DNS forward failed, ignore
            }
        }
    }

    private fun buildDnsResponsePacket(originalPacket: ByteArray, ipHeaderLength: Int, dnsResponse: ByteArray, dnsLength: Int): ByteArray {
        val totalLength = ipHeaderLength + UDP_HEADER_LENGTH + dnsLength
        val response = ByteArray(totalLength)

        // Copy IP header
        System.arraycopy(originalPacket, 0, response, 0, ipHeaderLength)

        // Swap IPs
        for (i in 0..3) {
            val temp = response[12 + i]
            response[12 + i] = response[16 + i]
            response[16 + i] = temp
        }

        // Copy UDP header
        System.arraycopy(originalPacket, ipHeaderLength, response, ipHeaderLength, UDP_HEADER_LENGTH)

        // Swap ports
        val srcPort = response[ipHeaderLength]
        val srcPort2 = response[ipHeaderLength + 1]
        response[ipHeaderLength] = response[ipHeaderLength + 2]
        response[ipHeaderLength + 1] = response[ipHeaderLength + 3]
        response[ipHeaderLength + 2] = srcPort
        response[ipHeaderLength + 3] = srcPort2

        // Copy DNS response
        System.arraycopy(dnsResponse, 0, response, ipHeaderLength + UDP_HEADER_LENGTH, dnsLength)

        finalizeIpUdpHeaders(response, ipHeaderLength, totalLength)

        return response
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(endTimeMillis: Long): Notification {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val endTimeStr = timeFormat.format(Date(endTimeMillis))

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, endTimeStr))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        soundPool?.release()
        soundPool = null
        super.onDestroy()
    }
}
