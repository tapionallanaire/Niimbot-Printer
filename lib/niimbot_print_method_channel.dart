import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:niimbot_print/constants/key_constant.dart';
import 'package:niimbot_print/constants/message_constant.dart';
import 'package:niimbot_print/constants/plugin_constant.dart';
import 'package:niimbot_print/helper/log_helper.dart';
import 'package:niimbot_print/model/blue_device_info_model.dart';
import 'package:niimbot_print/model/print_label_model.dart';
import 'package:niimbot_print/model/print_qr_code_model.dart';

import 'niimbot_print_platform_interface.dart';

class MethodChannelNiimbotPrint extends NiimbotPrintPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel(PluginConstant.niimbotPrint);
  @override
  Future<List<BlueDeviceInfoModel>> onStartScan(
      {Duration? scanDuration, Function(String)? onError}) async {
    try {
      final listString = await methodChannel.invokeListMethod<Object?>(
          PluginConstant.onStartScan,
          scanDuration?.inMilliseconds ??
              const Duration(seconds: 6).inMilliseconds);
      return (listString ?? const <Object?>[])
          .map((element) => BlueDeviceInfoModel.fromJson(
              jsonDecode(element.toString()) as Map<String, dynamic>))
          .toList(growable: false);
    } on PlatformException catch (e) {
      onError?.call(e.message ?? MessageConstant.scanFailed);
      LogHelper.error(e, event: PluginConstant.onStartScan);
    }
    return [];
  }

  @override
  Future<void> onStartConnect(
      {required BlueDeviceInfoModel model,
      required Function(bool isSuccess, String message) onResult}) async {
    try {
      final result = await methodChannel.invokeMethod<String>(
          PluginConstant.onStartConnect, jsonEncode(model.toJson()));
      if (result == KeyConstant.connectionSuccess) {
        onResult(true, MessageConstant.connectionSucceeded);
      } else if (result == KeyConstant.connectionFailed) {
        onResult(false, MessageConstant.connectionFailed);
      } else if (result == KeyConstant.failedPairing) {
        onResult(false, MessageConstant.failedPairing);
      } else {
        onResult(false, MessageConstant.unsupportedModels);
      }
    } on PlatformException catch (e) {
      LogHelper.error(e, event: PluginConstant.onStartConnect);
      onResult(false, e.message ?? MessageConstant.connectionFailed);
    }
  }

  @override
  Future<void> onStartPrintText(
      {required List<PrintLabelModel> printLabelModelList,
      required Function(bool isSuccess, String message) onResult}) async {
    try {
      final printableItems = printLabelModelList
          .where((element) => element.text?.trim().isNotEmpty ?? false)
          .toList(growable: false);
      final result = await methodChannel.invokeMethod<Object?>(
          PluginConstant.onStartPrintText,
          printableItems.map((e) => jsonEncode(e.toJson())).toList());
      if (result is bool) {
        if (result) {
          onResult(true, MessageConstant.printSucceed);
        } else {
          onResult(false, MessageConstant.printFailed);
        }
      } else {
        onResult(false, result?.toString() ?? MessageConstant.printFailed);
      }
    } on PlatformException catch (e) {
      LogHelper.error(e, event: PluginConstant.onStartPrintText);
      onResult(false, e.message ?? MessageConstant.printFailed);
    }
  }

  @override
  Future<void> onStartPrintQrCode(
      {required PrintQrCodeModel qrCode,
      required Function(bool isSuccess, String message) onResult}) async {
    try {
      final result = await methodChannel.invokeMethod<Object?>(
        PluginConstant.onStartPrintQrCode,
        jsonEncode(qrCode.toJson()),
      );
      if (result is bool && result) {
        onResult(true, MessageConstant.printSucceed);
      } else {
        onResult(false, result?.toString() ?? MessageConstant.printFailed);
      }
    } on PlatformException catch (error) {
      LogHelper.error(error, event: PluginConstant.onStartPrintQrCode);
      onResult(false, error.message ?? MessageConstant.printFailed);
    }
  }

  @override
  Future<void> onStartPrintRollLabel(
      {required String code,
      required List<String> lines,
      required double qrSizeMm}) async {
    await methodChannel.invokeMethod<Object?>(
      PluginConstant.onStartPrintRollLabel,
      jsonEncode(<String, Object?>{
        'code': code,
        'lines': lines,
        'qrSizeMm': qrSizeMm,
      }),
    );
  }

  @override
  Future<bool> onDisconnect() async {
    try {
      return await methodChannel
              .invokeMethod<bool>(PluginConstant.onDisconnect) ??
          false;
    } on PlatformException catch (e) {
      LogHelper.error(e, event: PluginConstant.onDisconnect);
      return false;
    }
  }

  @override
  Future<bool> isConnected() async {
    try {
      return await methodChannel
              .invokeMethod<bool>(PluginConstant.isConnected) ??
          false;
    } on PlatformException catch (e) {
      LogHelper.error(e, event: PluginConstant.isConnected);
      return false;
    }
  }
}
