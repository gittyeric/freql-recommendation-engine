package com.lmi.engine.worker.util.ignite

import java.util

import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.events.EventType
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder
import org.apache.ignite.{Ignite, Ignition}

object IgniteConfig extends Serializable {
	
	val LOCALHOST = "127.0.0.1"
	val FREQL_IGNITE_EVENT_TYPES =
		Array(
			EventType.EVT_CACHE_ENTRY_CREATED,
			EventType.EVT_CACHE_ENTRY_DESTROYED,
			EventType.EVT_CACHE_ENTRY_EVICTED,
			EventType.EVT_CACHE_OBJECT_PUT)
	
	def getOrCreateIgnite(
		                     igniteNodeIp: String = LOCALHOST,
		                     cnf: IgniteConfiguration =
		                     IgniteConfig.createIgniteConfig(IgniteConfig.getClusterConnectionConfig(LOCALHOST))): Ignite = {
		Ignition.getOrStart(cnf)
	}
	
	def createIgniteConfig(spi: TcpDiscoverySpi): IgniteConfiguration = {
		val cfg = new IgniteConfiguration()
		cfg.getAtomicConfiguration.setBackups(0)
		cfg.setIncludeEventTypes(FREQL_IGNITE_EVENT_TYPES: _*)
		cfg.setDiscoverySpi(spi)
		
		cfg
	}
	
	def getClusterConnectionConfig(rootIp: String): TcpDiscoverySpi = {
		val spi = new TcpDiscoverySpi()
		val ipFinder = new TcpDiscoveryVmIpFinder(true)
		ipFinder.setAddresses(util.Arrays.asList(rootIp))
		spi.setIpFinder(ipFinder)
		
		spi
	}
	
}
