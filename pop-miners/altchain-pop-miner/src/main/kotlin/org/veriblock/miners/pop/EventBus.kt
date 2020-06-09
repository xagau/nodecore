package org.veriblock.miners.pop

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import org.veriblock.core.utilities.AsyncEvent
import org.veriblock.core.utilities.EmptyEvent
import org.veriblock.core.utilities.Event
import org.veriblock.lite.core.Balance
import org.veriblock.lite.core.BlockChainReorganizedEventData
import org.veriblock.lite.core.FullBlock
import org.veriblock.lite.util.Threading
import org.veriblock.miners.pop.core.ApmOperation

object EventBus {

    val nodeCoreHealthyEvent = EmptyEvent("NodeCore Healthy")
    val nodeCoreUnhealthyEvent = EmptyEvent("NodeCore Unhealthy")
    val nodeCoreHealthySyncEvent = EmptyEvent("NodeCore Healthy Sync")
    val nodeCoreUnhealthySyncEvent = EmptyEvent("NodeCore Unhealthy Sync")
    val balanceChangedEvent = Event<Balance>("Balance Changed")

    val newBestBlockEvent = AsyncEvent<FullBlock>("New Best Block", Threading.LISTENER_THREAD)
    val newBestBlockChannel = BroadcastChannel<FullBlock>(Channel.CONFLATED)

    val blockChainReorganizedEvent = AsyncEvent<BlockChainReorganizedEventData>("Blockchain Reorganized", Threading.LISTENER_THREAD)

    val operationStateChangedEvent = AsyncEvent<ApmOperation>("Operation State Changed", Threading.MINER_THREAD)
    val operationFinishedEvent = AsyncEvent<ApmOperation>("Operation Finished", Threading.MINER_THREAD)
}
