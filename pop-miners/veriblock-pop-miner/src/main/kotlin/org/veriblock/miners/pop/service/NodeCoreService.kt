// VeriBlock PoP Miner
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package org.veriblock.miners.pop.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bitcoinj.utils.ContextPropagatingThreadFactory
import org.veriblock.core.utilities.createLogger
import org.veriblock.miners.pop.EventBus
import org.veriblock.miners.pop.NewVeriBlockFoundEventDto
import org.veriblock.miners.pop.VpmConfig
import org.veriblock.miners.pop.model.BlockStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

private val logger = createLogger {}

class NodeCoreService(
    private val config: VpmConfig,
    private val nodeCoreGateway: NodeCoreGateway,
    private val blockStore: BlockStore,
    bitcoinService: BitcoinService
) {
    private val healthy = AtomicBoolean(false)
    private val synchronized = AtomicBoolean(false)

    private val scheduledExecutorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ContextPropagatingThreadFactory("nc-poll")
    )

    private val coroutineScope = CoroutineScope(scheduledExecutorService.asCoroutineDispatcher())

    init {
        // Launching from the bitcoin context in order to propagate it
        bitcoinService.contextCoroutineScope.launch {
            coroutineScope.launch {
                delay(5000)
                while (true) {
                    poll()
                    delay(1000)
                }
            }
        }
    }

    fun isHealthy(): Boolean =
        healthy.get()

    fun isSynchronized(): Boolean =
        synchronized.get()

    fun shutdown() {
        scheduledExecutorService.shutdown()
        nodeCoreGateway.shutdown()
    }

    private fun poll() {
        try {
            if (isHealthy() && isSynchronized()) {
                if (!nodeCoreGateway.getNodeCoreStateInfo().isSynchronized) {
                    synchronized.set(false)
                    logger.info("The connected node is not synchronized")
                    EventBus.nodeCoreDesynchronizedEvent.trigger()
                    return
                }
                val latestBlock = try {
                    nodeCoreGateway.getLastBlock()
                } catch (e: Exception) {
                    logger.error("Unable to get the last block from NodeCore")
                    healthy.set(false)
                    EventBus.nodeCoreUnhealthyEvent.trigger()
                    return
                }
                val chainHead = blockStore.getChainHead()
                if (latestBlock != chainHead) {
                    blockStore.setChainHead(latestBlock)
                    EventBus.newVeriBlockFoundEvent.trigger(NewVeriBlockFoundEventDto(latestBlock, chainHead))
                }
            } else {
                if (nodeCoreGateway.ping()) {
                    val nodeCoreStateInfo = nodeCoreGateway.getNodeCoreStateInfo()

                    // Verify the NodeCore configured Network
                    if (!nodeCoreStateInfo.networkVersion.equals(config.bitcoin.network.name, true)) {
                        logger.info { "Network misconfiguration, VPM is configured at the ${config.bitcoin.network.name} network while NodeCore is at ${nodeCoreStateInfo.networkVersion}." }
                        return
                    }

                    if (!isHealthy()) {
                        logger.info("Connected to NodeCore")
                        EventBus.nodeCoreHealthyEvent.trigger()
                    }

                    healthy.set(true)
                    if (nodeCoreStateInfo.isSynchronized) {
                        if (!isSynchronized()) {
                            logger.info("The connected node is synchronized")
                            EventBus.nodeCoreSynchronizedEvent.trigger()
                        }
                        synchronized.set(true)
                    } else {
                        if (isSynchronized()) {
                            logger.info("The connected node is not synchronized")
                            EventBus.nodeCoreDesynchronizedEvent.trigger()
                        }
                        synchronized.set(false)
                    }
                } else {
                    if (isHealthy()) {
                        EventBus.nodeCoreUnhealthyEvent.trigger()
                    }
                    if (isSynchronized()) {
                        logger.info("The connected node is not synchronized")
                        EventBus.nodeCoreDesynchronizedEvent.trigger()
                    }
                    healthy.set(false)
                    synchronized.set(false)
                }
            }
        } catch (e: Exception) {
            logger.error("Error while polling NodeCore", e)
        }
    }
}
