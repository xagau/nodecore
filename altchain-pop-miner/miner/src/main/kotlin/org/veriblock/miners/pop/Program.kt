// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop

import org.koin.log.EmptyLogger
import org.koin.standalone.StandAloneContext.startKoin
import org.veriblock.miners.pop.plugins.pluguinModule
import org.veriblock.miners.pop.securityinheriting.SecurityInheritingService
import org.veriblock.miners.pop.shell.configure
import org.veriblock.miners.pop.storage.repositoryModule
import org.veriblock.miners.pop.storage.serviceModule
import org.veriblock.miners.pop.tasks.taskModule
import org.veriblock.sdk.createLogger
import org.veriblock.shell.Shell
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private val logger = createLogger {}

private val shutdownSignal = CountDownLatch(1)

private fun run(): Int {
    Runtime.getRuntime().addShutdownHook(Thread {
        shutdownSignal.countDown()
    })

    logger.info { "Starting dependency injection" }
    val koinApplication = startKoin(listOf(serviceModule, taskModule, minerModule, repositoryModule, pluguinModule), logger = EmptyLogger())

    val miner: Miner = koinApplication.koinContext.get()
    val securityInheritingService: SecurityInheritingService = koinApplication.koinContext.get()
    Shell.configure(miner)
    try {
        miner.start()
        securityInheritingService.start()
        Shell.run()
    } catch (e: Exception) {
        logger.warn(e) { "Error in shell: ${e.message}" }
    } finally {
        shutdownSignal.countDown()
    }

    try {
        shutdownSignal.await()
        securityInheritingService.stop()
        miner.shutdown()

        logger.info("Application exit")
    } catch (e: InterruptedException) {
        logger.error("Shutdown signal was interrupted", e)
        return 1
    } catch (e: Exception) {
        logger.error("Could not shut down services cleanly", e)
        return 1
    }

    return 0
}

fun main() {
    exitProcess(run())
}