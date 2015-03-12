 'use strict';
 
    var IDTech = ( typeof IDTech === 'undefined' ? {} : IDTech );
    var cordova = window.cordova || window.Cordova,
    
    IDTech.enable = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'enableReader', []);
    };
    IDTech.collect = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'getSwipe', []);
    };
    IDTech.cancel = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'cancelSwipe', []);
    };
    IDTech.disable = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'disableReader', []);
    };

    module.exports = IDTech;