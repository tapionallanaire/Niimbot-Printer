#import <Flutter/Flutter.h>
#import "NiimbotPrintPlugin.h"
#import "PluginConstant.h"
#import "PrintHelper.h"
#import "PrintUtility.h"

@implementation NiimbotPrintPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:niimbotPrint
            binaryMessenger:[registrar messenger]];
  NiimbotPrintPlugin* instance = [[NiimbotPrintPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {

  PrintHelper *printHelper = [[PrintHelper alloc] init];
  if ([onStartScan isEqualToString:call.method]) {
    [printHelper onStartScan:call result:result];
  } else if ([onStartConnect isEqualToString:call.method]) {
    [printHelper onStartConnect:call result:result];
  } else if ([onStartPrintText isEqualToString:call.method]) {
    [printHelper onStartPrintText:call result:result];
  } else if ([onStartPrintQrCode isEqualToString:call.method]) {
    [printHelper onStartPrintQrCode:call result:result];
  } else if ([onStartPrintRollLabel isEqualToString:call.method]) {
    result([FlutterError errorWithCode:@"UNIMPLEMENTED"
                                message:@"onStartPrintRollLabel is not implemented on iOS"
                                details:nil]);
  } else if ([onDisconnect isEqualToString:call.method]) {
    [printHelper onDisconnect:result];
  } else if ([isConnected isEqualToString:call.method]) {
    [printHelper isConnected:result];
  } else if ([getAndroidSdkInt isEqualToString:call.method]) {
    result(@0);
  } else {
    result(FlutterMethodNotImplemented);
  }
}

@end
