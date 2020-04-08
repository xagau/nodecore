// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import org.veriblock.miners.pop.api.model.*
import org.veriblock.miners.pop.service.MinerService

class MiningController(
    private val minerService: MinerService
) : ApiController {

    @Path("/api/operations")
    class MinerOperationsPath

    @Path("/api/operations/{id}")
    class MinerOperationPath(
        @PathParam("Operation ID") val id: String
    )

    @Path("/api/mine")
    class MineActionPath

    @Path("/api/miner")
    class MinerPath

    override fun NormalOpenAPIRoute.registerApi() {
        get<MinerOperationsPath, OperationSummaryListResponse>(
            info("Get operations list")
        ) {
            val operationSummaries = minerService.listOperations()

            val responseModel = operationSummaries.map { it.toResponse() }
            respond(OperationSummaryListResponse(responseModel))
        }
        get<MinerOperationPath, OperationDetailResponse>(
            info("Get operation details")
        ) { location ->
            val id = location.id

            val operationState = minerService.getOperation(id)
                ?: throw NotFoundException("Operation $id not found")

            val responseModel = operationState.toResponse()
            respond(responseModel)
        }
        post<MineActionPath, MineResultResponse, MineRequest>(
            info("Start mining operation")
        ) { _, request ->
            val result = minerService.mine(request.block)

            val responseModel = result.toResponse()
            respond(responseModel)
        }
        get<MinerPath, MinerInfoResponse>(
            info("Get miner data")
        ) {
            val responseModel = MinerInfoResponse(
                bitcoinBalance = minerService.getBitcoinBalance().longValue(),
                bitcoinAddress = minerService.getBitcoinReceiveAddress(),
                minerAddress = minerService.getMinerAddress(),
                walletSeed = minerService.getWalletSeed()
            )
            respond(responseModel)
        }
    }
}
