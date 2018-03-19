package com.lmi.examples.whitepaper

import com.lmi.engine.worker.util.ignite.IgniteConfig
import org.apache.ignite.Ignition
import org.apache.ignite.configuration.DataStorageConfiguration

object Ignite {
	
	// Create a default Ignite Config but with persistence turned on to save RAM
	def createConfiguredIgnite() = {
		val tcpConfig = IgniteConfig.getClusterConnectionConfig(IgniteConfig.LOCALHOST)
		val config = IgniteConfig.createIgniteConfig(tcpConfig)
		
		val storageCfg = new DataStorageConfiguration
		storageCfg.getDefaultDataRegionConfiguration.setPersistenceEnabled(true)
		storageCfg.setStoragePath("")
		storageCfg.setWalArchivePath("")
		storageCfg.setWalPath("")
		config.setDataStorageConfiguration(storageCfg)
		
		config.setAutoActivationEnabled(true)
		config.setActiveOnStart(true)
		val ignite = Ignition.getOrStart(config)
		ignite.active(true)
		
		ignite
	}
	
}
