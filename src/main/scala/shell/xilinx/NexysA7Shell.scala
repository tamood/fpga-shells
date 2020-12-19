// See LICENSE for license details.
package sifive.fpgashells.shell.xilinx.nexysA7shell

import Chisel._
import chisel3.{Input, Output, RawModule, withClockAndReset}
import chisel3.experimental.{attach, Analog, IntParam}

import freechips.rocketchip.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.util.{ElaborationArtefacts}
import freechips.rocketchip.jtag.{JTAGIO}

import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.clocks._

//-------------------------------------------------------------------------
// NexysA7Shell
//-------------------------------------------------------------------------

abstract class NexysA7Shell extends RawModule {

  //-----------------------------------------------------------------------
  // Interface
  //-----------------------------------------------------------------------

  // Clock & Reset
  val clock				= IO(Input(Clock()))
  val resetn			= IO(Input(Bool()))

  // LEDs
  val LED				= IO(Output(UInt(16.W)))

  //Buttons
  val BTN				= IO(new Bundle {
									val U = Input(Bool())
									val D = Input(Bool())
									val L = Input(Bool())
									val R = Input(Bool())
									val C = Input(Bool())})
  
  //RGB LEDs
  val RGB				= IO(Vec(2, new Bundle {
										val R = Output(Bool())
										val G = Output(Bool())
										val B = Output(Bool())}))
										
  //Sliding switches
  val SW				= IO(Input(UInt(16.W)))
  
  //PMOD Headers (Connected J_1 .. J_4, J_7 .. J_10)
  val JA				= IO(Vec(12, Analog(1.W)))
  val JB				= IO(Vec(12, Analog(1.W)))
  val JC				= IO(Vec(12, Analog(1.W)))
  val JD				= IO(Vec(12, Analog(1.W)))  
  val JX				= IO(Vec(12, Analog(1.W)))
  

  //7 segmant display
  val AN				= IO(Output(UInt(8.W)))
  val CA				= IO(Output(UInt(8.W)))
  
  //VGA connectivity
  val VGA				= IO(new Bundle {
									val HSYNC	= Output(Bool())
									val VSYNC	= Output(Bool())
									val RGB		= Output(UInt(12.W))})
  
  //Accelerometer
  val ACL				= IO(new Bundle {
									val MISO	= Analog(1.W)
									val MOSI	= Analog(1.W)
									val SCLK	= Analog(1.W)
									val CSN		= Analog(1.W)})
  
  //Temperature Sensor
  val TMP				= IO(new Bundle {
									val SDA	= Analog(1.W)
									val SCL	= Analog(1.W)})

  // UART
  val UART				= IO(new Bundle {
									val TX		= Analog(1.W)
									val RX		= Analog(1.W)
									val RTSN	= Output(Bool())
									val CTSN	= Input(Bool())})
  //Audio
  val AUD				= IO(new Bundle {
									val PWM	= Analog(1.W)
									val SDN	= Output(Bool())})
									
  // NOR Flash
  val QSPI_CSN			= IO(Analog(1.W))
  val QSPI_DQ			= IO(Vec(4, Analog(1.W)))
  
  //Microsd x1
  val SD				= IO(new Bundle {
									val MISO	= Analog(1.W)
									val MOSI	= Analog(1.W)
									val CLK	    = Analog(1.W)
									val CS		= Analog(1.W)
									val RST		= Output(Bool())})
									
}
