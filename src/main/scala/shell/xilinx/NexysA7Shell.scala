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

class BSCANTunnel extends RawModule {
	
  val io = IO(new JTAGIO(false))

  val BSCANE2_inst = Module(new BSCANE2)
  
  val rst = ~(BSCANE2_inst.io.SEL & BSCANE2_inst.io.SHIFT)
  val negEdge_clk = ~(BSCANE2_inst.io.TCK.asUInt.asBool)
  
  val counter_neg = withClockAndReset(negEdge_clk.asClock, rst){
	  val cnt = RegInit(0.U(8.W))
	  cnt := cnt + 1.U
	  cnt
  }
  
  val counter_pos = withClockAndReset(BSCANE2_inst.io.TCK, rst){
	  val cnt = RegInit(0.U(8.W))
	  cnt := cnt + 1.U
	  cnt
  }
  
  val TDI_REG = withClockAndReset(BSCANE2_inst.io.TCK, rst){
	  val r = RegInit(false.B)
	  when(counter_pos === 0.U)
	  {
		  r := ~BSCANE2_inst.io.TDI
	  }
	  r
  }
  
  val shiftreg_cnt = withClockAndReset(BSCANE2_inst.io.TCK, rst){
	  val sh = RegInit(0.U(7.W))
	  when((counter_pos >= 1.U) && (counter_pos <= 7.U))
	  {
		  sh := Cat(BSCANE2_inst.io.TDI, sh(6, 1))
	  }
	  sh
  }
  
  val jtag_tms = Wire(Bool())
  jtag_tms := false.B
  when(counter_neg === 4.U)
  {
	  jtag_tms := TDI_REG
  }
  .elsewhen(counter_neg === 5.U)
  {
	  jtag_tms := true.B
  }
  .elsewhen((counter_neg === (8.U(8.W) + shiftreg_cnt)) || (counter_neg === (7.U(8.W) + shiftreg_cnt)))
  {
	  jtag_tms := true.B
  }
  
  io.TCK := BSCANE2_inst.io.TCK
  io.TMS := jtag_tms
  io.TDI := BSCANE2_inst.io.TDI
  BSCANE2_inst.io.TDO := Mux(io.TDO.driven, io.TDO.data, true.B)
							
}

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
  val JC				= IO(Vec(12, Analog(1.W)))
  val JD				= IO(Vec(12, Analog(1.W)))  
  val JX				= IO(Vec(12, Analog(1.W)))
  
  val JB				= IO(Vec(10, Analog(1.W))) 
  val JB_10				= IO(Input(Clock()))

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
									
}
