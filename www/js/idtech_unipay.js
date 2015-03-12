 'use strict';
 
    var IDTech = ( typeof IDTech === 'undefined' ? {} : IDTech );
    var cordova = window.cordova || window.Cordova,
 
    fail = function(error) {
        console.log('Error running your request: ' + error);
    };
    
   IDTech.fnord = function(callback, error) {
        var success = function(fnord) {
        	console.log(fnord)
        	callback(fnord);
        };
        var fail_handler =  fail;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'fnord', []);
    };

    
    IDTech.enable = function(callback, error) {
        var success = function(fnord) {
        	console.log(fnord)
        	callback(fnord);
        };
        var fail_handler =  fail;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'enableReader', []);
    };
    IDTech.collect = function(callback, error) {
        var success = function(fnord) {
        	console.log(fnord)
        	callback(fnord);
        };
        var fail_handler =  fail;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'getSwipe', []);
    };
    IDTech.cancel = function(callback, error) {
        var success = function(fnord) {
        	console.log(fnord)
        	callback(fnord);
        };
        var fail_handler =  fail;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'cancelSwipe', []);
    };
    IDTech.disable = function(callback, error) {
        var success = function(fnord) {
        	console.log(fnord)
        	callback(fnord);
        };
        var fail_handler =  fail;
        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'disableReader', []);
    };
    
    

    module.exports = IDTech;