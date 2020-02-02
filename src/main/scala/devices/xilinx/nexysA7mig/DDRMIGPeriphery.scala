// See LICENSE for license details.
package sifive.fpgashells.devices.xilinx.nexysA7mig

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, AddressRange}
import sifive.fpgashells.ip.xilinx.nexysA7mig._
case object PeripheryDDRMIGKey extends Field[DDRMIGParams]

trait HasDDRMIG { this: BaseSubsystem =>
  val module: HasDDRMIGModuleImp

  val ddrmigio = LazyModule(new NexysA7MIG(p(PeripheryDDRMIGKey)))

  ddrmigio.node := mbus.toDRAMController(Some("ddrmig"))()
}

trait HasDDRMIGBundle {
  val ddrmigio: DDRMIGIO
  def connectDDRMIGToPads(pads: DDRMIGPads) {
    pads <> ddrmigio
  }
}

trait HasDDRMIGModuleImp extends LazyModuleImp
    with HasDDRMIGBundle {
  val outer: HasDDRMIG
  val ranges = AddressRange.fromSets(p(PeripheryDDRMIGKey).address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val depth = ranges.head.size
  val ddrmigio = IO(new DDRMIGIO)

  ddrmigio <> outer.ddrmigio.module.io.port
}
