var exec = require('cordova/exec');

exports.open = function(device,rate, success, error) {
    exec(success, error, "cordova-plugin-serialport", "open", [device,rate]);
};
exports.write = function(data, success, error) {
    exec(success, error, "cordova-plugin-serialport", "write", [data]);
};
exports.close = function(success, error) {
    exec(success, error, "cordova-plugin-serialport", "close", []);
};
exports.watch = function(success, error) {
    exec(success, error, "cordova-plugin-serialport", "watch", []);
};