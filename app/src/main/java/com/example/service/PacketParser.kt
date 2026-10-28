package com.example.service

import java.net.InetAddress
import java.nio.ByteBuffer

data class ParsedPacketInfo(
    val ipVersion: Int, // 4 or 6
    val protocolName: String, // "TCP", "UDP", "ICMP", "DNS"
    val srcIp: String,
    val dstIp: String,
    val dstPort: Int,
    val summaryAr: String
)

object PacketParser {

    fun parse(bytes: ByteArray, length: Int): ParsedPacketInfo? {
        if (length < 20) return null

        val version = (bytes[0].toInt() shr 4) and 0x0F

        return when (version) {
            4 -> parseIpv4(bytes, length)
            6 -> parseIpv6(bytes, length)
            else -> null
        }
    }

    private fun parseIpv4(bytes: ByteArray, length: Int): ParsedPacketInfo? {
        if (length < 20) return null
        val ihl = (bytes[0].toInt() and 0x0F) * 4
        val protocol = bytes[9].toInt() and 0xFF

        val srcIp = try {
            InetAddress.getByAddress(bytes.sliceArray(12..15)).hostAddress ?: "0.0.0.0"
        } catch (_: Exception) { "0.0.0.0" }

        val dstIp = try {
            InetAddress.getByAddress(bytes.sliceArray(16..19)).hostAddress ?: "0.0.0.0"
        } catch (_: Exception) { "0.0.0.0" }

        var dstPort = 0
        var protoName = when (protocol) {
            6 -> "TCP"
            17 -> "UDP"
            1 -> "ICMP"
            else -> "IP($protocol)"
        }

        if (length >= ihl + 4 && (protocol == 6 || protocol == 17)) {
            dstPort = ((bytes[ihl + 2].toInt() and 0xFF) shl 8) or (bytes[ihl + 3].toInt() and 0xFF)
            if (dstPort == 53) {
                protoName = "DNS"
            } else if (dstPort == 443) {
                protoName = "$protoName (HTTPS)"
            } else if (dstPort == 80) {
                protoName = "$protoName (HTTP)"
            }
        }

        val summary = if (dstPort > 0) {
            "حظر $protoName إلى $dstIp:$dstPort"
        } else {
            "حظر بروتوكول $protoName إلى $dstIp"
        }

        return ParsedPacketInfo(
            ipVersion = 4,
            protocolName = protoName,
            srcIp = srcIp,
            dstIp = dstIp,
            dstPort = dstPort,
            summaryAr = summary
        )
    }

    private fun parseIpv6(bytes: ByteArray, length: Int): ParsedPacketInfo? {
        if (length < 40) return null
        val nextHeader = bytes[6].toInt() and 0xFF

        val srcIp = try {
            InetAddress.getByAddress(bytes.sliceArray(8..23)).hostAddress ?: "::"
        } catch (_: Exception) { "::" }

        val dstIp = try {
            InetAddress.getByAddress(bytes.sliceArray(24..39)).hostAddress ?: "::"
        } catch (_: Exception) { "::" }

        var dstPort = 0
        var protoName = when (nextHeader) {
            6 -> "TCP"
            17 -> "UDP"
            58 -> "ICMPv6"
            else -> "IPv6($nextHeader)"
        }

        if (length >= 44 && (nextHeader == 6 || nextHeader == 17)) {
            dstPort = ((bytes[42].toInt() and 0xFF) shl 8) or (bytes[43].toInt() and 0xFF)
            if (dstPort == 53) protoName = "DNS"
        }

        val summary = if (dstPort > 0) {
            "حظر $protoName (IPv6) إلى $dstIp:$dstPort"
        } else {
            "حظر $protoName (IPv6) إلى $dstIp"
        }

        return ParsedPacketInfo(
            ipVersion = 6,
            protocolName = protoName,
            srcIp = srcIp,
            dstIp = dstIp,
            dstPort = dstPort,
            summaryAr = summary
        )
    }
}
