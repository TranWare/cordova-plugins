 'use strict';
 
    var IDTech = ( typeof IDTech === 'undefined' ? {} : IDTech );
    var cordova = window.cordova || window.Cordova;
    
    IDTech.detectReader = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'ACTION_DETECT_READER', []);
    };
    
    IDTech.getSwipe = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'ACTION_GET_SWIPE', []);
    };
    
    IDTech.cancelSwipe = function(callback, error) {
        var success = function(data) {
        	console.log(data)
        	callback(data);
        };
        var fail_handler =  error;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'ACTION_CANCEL_SWIPE', []);
    };

    module.exports = IDTech;