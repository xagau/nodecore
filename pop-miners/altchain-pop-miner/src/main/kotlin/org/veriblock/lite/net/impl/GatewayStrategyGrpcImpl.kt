// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.lite.net.impl

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import nodecore.api.grpc.AdminGrpc
import nodecore.api.grpc.VeriBlockMessages
import org.veriblock.lite.net.GatewayStrategy
import org.veriblock.lite.params.NetworkParameters
import org.veriblock.lite.serialization.deserialize
import java.util.concurrent.TimeUnit

class GatewayStrategyGrpcImpl(
    private val blockingStub: AdminGrpc.AdminBlockingStub,
    private val channel: ManagedChannel,
    private val networkParameters: NetworkParameters
) : GatewayStrategy {

    override fun getBalance(getBalanceRequest: VeriBlockMessages.GetBalanceRequest): VeriBlockMessages.GetBalanceReply {
        return blockingStub
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .getBalance(getBalanceRequest)
    }

    override fun getVeriBlockPublications(getVeriBlockPublicationsRequest: VeriBlockMessages.GetVeriBlockPublicationsRequest): VeriBlockMessages.GetVeriBlockPublicationsReply {
        return blockingStub
            .withDeadlineAfter(300, TimeUnit.SECONDS)
            .getVeriBlockPublications(getVeriBlockPublicationsRequest)
    }

    override fun getDebugVeriBlockPublications(getDebugVTBsRequest: VeriBlockMessages.GetDebugVTBsRequest): VeriBlockMessages.GetDebugVTBsReply {
        return blockingStub
            .withDeadlineAfter(300, TimeUnit.SECONDS)
            .getDebugVTBs(getDebugVTBsRequest)
    }

    override fun ping(pingRequest: VeriBlockMessages.PingRequest): VeriBlockMessages.PingReply {
        return blockingStub
            .withDeadlineAfter(5L, TimeUnit.SECONDS)
            .ping(pingRequest)
    }

    override fun getNodeCoreSyncStatus(getStateInfoRequest: VeriBlockMessages.GetStateInfoRequest): VeriBlockMessages.GetStateInfoReply {
        return blockingStub
            .withDeadlineAfter(5L, TimeUnit.SECONDS)
            .getStateInfo(getStateInfoRequest)
    }

    override fun submitTransactions(submitTransactionsRequest: VeriBlockMessages.SubmitTransactionsRequest): VeriBlockMessages.ProtocolReply {
        return blockingStub
            .withDeadlineAfter(3, TimeUnit.SECONDS)
            .submitTransactions(submitTransactionsRequest)
    }

    override fun createAltChainEndorsement(altChainEndorsementRequest: VeriBlockMessages.CreateAltChainEndorsementRequest): VeriBlockMessages.CreateAltChainEndorsementReply {
        return blockingStub
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .createAltChainEndorsement(altChainEndorsementRequest)
    }

    override fun getLastVBKBlockHeader(): VeriBlockMessages.BlockHeader {
        val lastBlock = blockingStub.getLastBlock(VeriBlockMessages.GetLastBlockRequest.getDefaultInstance())
        return lastBlock.header
    }

    override fun getVBKBlockHeader(blockHash: ByteArray): VeriBlockMessages.BlockHeader {
        val request = VeriBlockMessages.GetBlocksRequest.newBuilder()
            .addFilters(
                VeriBlockMessages.BlockFilter.newBuilder()
                    .setHash(ByteString.copyFrom(blockHash))
                    .build()
            )
            .build()

        val reply = blockingStub
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .getBlocks(request)

        if (reply.success && reply.blocksCount > 0) {
            val block = reply.getBlocks(0)
            val bytesHeader = block.deserialize(networkParameters.transactionPrefix).raw
            return VeriBlockMessages.BlockHeader
                .newBuilder()
                .setHash(block.hash)
                .setHeader(ByteString.copyFrom(bytesHeader))
                .build()
        }

        return VeriBlockMessages.BlockHeader.getDefaultInstance()
    }

    override fun getTransactions(request: VeriBlockMessages.GetTransactionsRequest?): VeriBlockMessages.GetTransactionsReply? {
        return blockingStub
            .withDeadlineAfter(300, TimeUnit.SECONDS)
            .getTransactions(request)
    }

    override fun shutdown() {
        channel.shutdown().awaitTermination(15, TimeUnit.SECONDS)
    }
}
