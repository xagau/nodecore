// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.alt.plugins

import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import org.veriblock.sdk.AltPublication
import org.veriblock.sdk.Configuration
import org.veriblock.sdk.PublicationData
import org.veriblock.sdk.VeriBlockPublication
import org.veriblock.sdk.alt.FamilyPluginSpec
import org.veriblock.sdk.alt.PublicationDataWithContext
import org.veriblock.sdk.alt.SecurityInheritingChain
import org.veriblock.sdk.asHexBytes
import org.veriblock.sdk.createLogger
import org.veriblock.sdk.services.SerializeDeserializeService
import org.veriblock.sdk.toHex
import org.veriblock.sdk.util.Base58

private val logger = createLogger {}

class BtcConfig(
    val host: String = "http://localhost:8332",
    val auth: HttpAuthConfig? = null,
    val autoMine: BtcAutoMineConfig? = null
)

class BtcAutoMineConfig(
    val round1: Boolean = false,
    val round2: Boolean = false,
    val round3: Boolean = false,
    val round4: Boolean = false
)

private data class BtcPublicationData(
    val block_header: String,
    val raw_contextinfocontainer: String,
    val last_known_veriblock_blocks: List<String>,
    val last_known_bitcoin_blocks: List<String>
)

@FamilyPluginSpec(name = "BitcoinFamily", key = "btc")
class BitcoinFamilyChain(
    val id: Long,
    val key: String,
    val name: String
) : SecurityInheritingChain {

    private val config = Configuration.extract("securityInheriting.$key") ?: BtcConfig()

    private val httpClient = createHttpClient(config.auth)

    override fun getChainIdentifier(): Long {
        return id
    }

    override fun shouldAutoMine(): Boolean {
        return config.autoMine != null && (config.autoMine.round1 || config.autoMine.round2 || config.autoMine.round3 || config.autoMine.round4)
    }

    override fun shouldAutoMine(blockHeight: Int): Boolean {
        // TODO proper round calculation for each alt
        val round = blockHeight % 5
        return config.autoMine != null && (
            (round == 1 && config.autoMine.round1) ||
            (round == 2 && config.autoMine.round2) ||
            (round == 3 && config.autoMine.round3) ||
            (round == 4 && config.autoMine.round4)
        )
    }

    override fun getBestBlockHeight(): Int = runBlocking {
        logger.info { "Retrieving best block height..." }
        val jsonBody = JsonRpcRequestBody("getblockcount").toJson()
        httpClient.post<RpcResponse>(config.host) {
            body = jsonBody
        }.handle<Int>()
    }

    override fun getPublicationData(blockHeight: Int?): PublicationDataWithContext = runBlocking {
        val actualBlockHeight = blockHeight
            // Retrieve top block height from API if not supplied
            ?: getBestBlockHeight()

        logger.info { "Retrieving publication data at height $actualBlockHeight from $key daemon at ${config.host}..." }
        val jsonBody = JsonRpcRequestBody("getpopdata", listOf(actualBlockHeight)).toJson()
        val response: BtcPublicationData = httpClient.post<RpcResponse>(config.host) {
            body = jsonBody
        }.handle()

        val publicationData = PublicationData(
            getChainIdentifier(),
            response.block_header.asHexBytes(),
            Base58.decode("VFMJSUgJCy9QRa1RjXNmJ5kLy5D35C"), // TODO retrieve from response
            response.raw_contextinfocontainer.asHexBytes()
        )
        if (response.last_known_veriblock_blocks.isEmpty()) {
            error("Publication data's context (last known VeriBlock blocks) must not be empty!")
        }
        PublicationDataWithContext(
            publicationData,
            response.last_known_veriblock_blocks.map { it.asHexBytes() },
            response.last_known_bitcoin_blocks.map { it.asHexBytes() }
        )
    }

    override fun submit(proofOfProof: AltPublication, veriBlockPublications: List<VeriBlockPublication>): String = runBlocking {
        logger.info { "Submitting PoP and VeriBlock publications to $key daemon at ${config.host}..." }
        val jsonBody = JsonRpcRequestBody("submitpop", listOf(
            SerializeDeserializeService.serialize(proofOfProof).toHex(),
            veriBlockPublications.map { SerializeDeserializeService.serialize(it).toHex() }
        )).toJson()

        httpClient.post<String>(config.host) {
            body = jsonBody
        }
    }
}
