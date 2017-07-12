var exec = require('cordova/exec');

exports.open = function (device, rate, success, error) {
    exec(success, error, "cordova-plugin-serialport", "open", [device, rate]);
};
exports.write = function (arrayBuffer, success, error) {
    exec(success, error, "cordova-plugin-serialport", "write", [arrayBuffer]);
};
exports.writeText = function (text, success, error) {
    exec(success, error, "cordova-plugin-serialport", "writeText", [text]);
};
exports.close = function (success, error) {
    exec(success, error, "cordova-plugin-serialport", "close", []);
};
exports.register = function (success, error) {
    exec(success, error, "cordova-plugin-serialport", "register", []);
};