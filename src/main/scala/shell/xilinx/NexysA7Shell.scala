// See LICENSE for license details.
package sifive.fpgashells.shell.xilinx.nexysA7shell

import Chisel._
import chisel3.core.{Input, Output, attach}
import chisel3.experimental.{RawModule, Analog, withClockAndReset}
import freechips.rocketchip.util.{ElaborationArtefacts}

import freechips.rocketchip.config._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.util._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.spi._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.chiplink._

import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.clocks._

class nexysA7reset() extends BlackBox
{
  val io = new Bundle{
    val areset = Bool(INPUT)
    val clock1 = Clock(INPUT)
    val reset1 = Bool(OUTPUT)
    val clock2 = Clock(INPUT)
    val reset2 = Bool(OUTPUT)
  }
}

class VGA() extends BlackBox
{
  val io = new Bundle{
    val clk = Clock(INPUT)
    val rst = Bool(INPUT)
    val hsync = Bool(OUTPUT)
    val vsync = Bool(OUTPUT)
    val rgb = Output(UInt(12.W))
  }
}

class BUFG extends BlackBox {
  val io = IO(new Bundle {
    val O = Output(Bool())
    val I = Input(Bool())
  })
}

object BUFG {
  def apply(pin: Bool): Bool = {
    val pad = Module (new BUFG)
    pad.io.I := pin
    pad.io.O
  }
}

abstract class NexysA7Shell(implicit val p: Parameters) extends RawModule {

  //-----------------------------------------------------------------------
  // Interface
  //-----------------------------------------------------------------------

  // clock
  val clock = IO(Input(Clock()))
  val resetn = IO(Input(Bool()))

  // LEDs
  val LED                  = IO(Output(UInt(16.W)))

  // UART
  val uart_tx              = IO(Output(Bool()))
  val uart_rx              = IO(Input(Bool()))
  val uart_rtsn            = IO(Output(Bool()))
  val uart_ctsn            = IO(Input(Bool()))

  // SDIO
  val sd_reset             = IO(Output(Bool()))
  val sdio_clk             = IO(Output(Bool()))
  val sdio_cmd             = IO(Analog(1.W))
  val sdio_dat             = IO(Analog(4.W))

  //Buttons
  val btn_u                = IO(Input(Bool()))
  val btn_d                = IO(Input(Bool()))
  val btn_l                = IO(Input(Bool()))
  val btn_r                = IO(Input(Bool()))
  val btn_c                = IO(Input(Bool()))
  
  //RGB LEDs
  val rgb0_r               = IO(Output(Bool()))
  val rgb0_g               = IO(Output(Bool()))
  val rgb0_b               = IO(Output(Bool()))
  val rgb1_r               = IO(Output(Bool()))
  val rgb1_g               = IO(Output(Bool()))
  val rgb1_b               = IO(Output(Bool()))

  //Sliding switches
  val SW                   = IO(Input(UInt(16.W)))
  
  val JA                   = IO(Vec(12, Analog(1.W)))
  val JB                   = IO(Vec(12, Analog(1.W)))
  val JC                   = IO(Vec(12, Analog(1.W)))
  val JD                   = IO(Vec(12, Analog(1.W)))
  
  val AN                   = IO(Output(UInt(8.W)))
  val CA                   = IO(Output(UInt(8.W)))

  val vga_hsync            = IO(Output(Bool()))
  val vga_vsync            = IO(Output(Bool()))
  val vga_rgb              = IO(Output(UInt(12.W)))
  
  //-----------------------------------------------------------------------
  // Wire declrations
  //-----------------------------------------------------------------------

  val dut_clock       = Wire(Clock())
  val dut_reset       = Wire(Bool())

  val sd_spi_sck      = Wire(Bool())
  val sd_spi_cs       = Wire(Bool())
  val sd_spi_dq_i     = Wire(Vec(4, Bool()))
  val sd_spi_dq_o     = Wire(Vec(4, Bool()))

  val mig_clock_gen_locked = Wire(Bool())
  val migaresetn   = Wire(Bool())
  val mig_ui_clock    = Wire(Clock())
  val mig_ui_reset    = Wire(Bool())

  //-----------------------------------------------------------------------
  // Clock Generator
  //-----------------------------------------------------------------------

  val clock_gen = Module(new Series7MMCM(PLLParameters("MASTER_CLOCK_GEN",
    PLLInClockParameters(100, 50),
    Seq(
      PLLOutClockParameters(200),
      PLLOutClockParameters(50)))))
  
  clock_gen.io.clk_in1 := IBUFG(clock)
  clock_gen.io.reset   := ~resetn
  val clock_gen_locked = clock_gen.io.locked
  val Seq(clk200Mhz, busclk, _*) = clock_gen.getClocks

  // clocks
  dut_clock := busclk
  mig_ui_clock := dut_clock

  //-----------------------------------------------------------------------
  // System reset
  //-----------------------------------------------------------------------
  
  mig_clock_gen_locked      := true.B

  val safe_reset = Module(new nexysA7reset)

  safe_reset.io.areset := !clock_gen_locked || !mig_clock_gen_locked || mig_ui_reset
  safe_reset.io.clock1 := mig_ui_clock
  migaresetn           := BUFG(~safe_reset.io.reset1)
  safe_reset.io.clock2 := dut_clock
  dut_reset            := BUFG(safe_reset.io.reset2)

  //-----------------------------------------------------------------------
  // UART
  //-----------------------------------------------------------------------

  uart_rtsn := false.B

  def connectUART(dut: HasPeripheryUARTModuleImp): Unit = {
    val uartParams = p(PeripheryUARTKey)
    if (!uartParams.isEmpty) {
      dut.uart(0).rxd := uart_rx
      uart_tx         := dut.uart(0).txd
    }
  }

  //-----------------------------------------------------------------------
  // SPI
  //-----------------------------------------------------------------------

  sd_reset := false.B

  def connectSPI(dut: HasPeripherySPIModuleImp): Unit = {
    // SPI
    sd_spi_sck := dut.spi(0).sck
    sd_spi_cs  := dut.spi(0).cs(0)

    dut.spi(0).dq.zipWithIndex.foreach {
      case(pin, idx) =>
        sd_spi_dq_o(idx) := pin.o
        pin.i            := sd_spi_dq_i(idx)
    }

    //-------------------------------------------------------------------
    // SDIO <> SPI Bridge
    //-------------------------------------------------------------------

    val ip_sdio_spi = Module(new sdio_spi_bridge())

    ip_sdio_spi.io.clk   := dut_clock
    ip_sdio_spi.io.reset := dut_reset

    // SDIO
    attach(sdio_dat, ip_sdio_spi.io.sd_dat)
    attach(sdio_cmd, ip_sdio_spi.io.sd_cmd)
    sdio_clk := sd_spi_sck

    // SPI
    ip_sdio_spi.io.spi_sck  := sd_spi_sck
    ip_sdio_spi.io.spi_cs   := sd_spi_cs
    sd_spi_dq_i             := ip_sdio_spi.io.spi_dq_i.toBools
    ip_sdio_spi.io.spi_dq_o := sd_spi_dq_o.asUInt
  }
  
  //-------------------------------------------------------------------
  // VGA Stub
  //-------------------------------------------------------------------
  
  val inst_vga = Module(new VGA)
  vga_hsync := inst_vga.io.hsync
  vga_vsync := inst_vga.io.vsync
  vga_rgb := inst_vga.io.rgb
  inst_vga.io.clk := dut_clock
  inst_vga.io.rst := dut_reset
  
  ElaborationArtefacts.add(
    "clockdomains.synth.tcl",
    """
    create_clock -add -name sys_clk -period 10.00 -waveform {0 5} [get_ports {clock}]
    set_clock_groups -asynchronous -group [get_clocks -include_generated_clocks sys_clk]
    set_clock_groups -asynchronous -group [get_clocks clk_out1*]
    set_clock_groups -asynchronous -group [get_clocks clk_out2*]
    """)

}
