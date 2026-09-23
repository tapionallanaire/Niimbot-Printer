import 'package:niimbot_print/constants/message_constant.dart';
import 'package:niimbot_print/model/blue_device_info_model.dart';
import 'package:niimbot_print/model/print_label_model.dart';
import 'package:niimbot_print/model/print_qr_code_model.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'niimbot_print_method_channel.dart';

abstract class NiimbotPrintPlatform extends PlatformInterface {
  NiimbotPrintPlatform() : super(token: _token);

  static final Object _token = Object();

  static NiimbotPrintPlatform _instance = MethodChannelNiimbotPrint();

  static NiimbotPrintPlatform get instance => _instance;

  static set instance(NiimbotPrintPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<BlueDeviceInfoModel>> onStartScan(
      {Duration? scanDuration, Function(String)? onError}) async {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }

  Future<void> onStartConnect(
      {required BlueDeviceInfoModel model,
      required Function(bool isSuccess, String message) onResult}) async {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }

  Future<void> onStartPrintText(
      {required List<PrintLabelModel> printLabelModelList,
      required Function(bool isSuccess, String message) onResult}) async {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }

  Future<void> onStartPrintQrCode(
      {required PrintQrCodeModel qrCode,
      required Function(bool isSuccess, String message) onResult}) async {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }

  Future<void> onStartPrintRollLabel(
      {required String code,
      required List<String> lines,
      required double qrSizeMm}) async {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }

  Future<bool> onDisconnect() {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }

  Future<bool> isConnected() {
    throw UnimplementedError(MessageConstant.errorPlatformNotImplemented);
  }
}
