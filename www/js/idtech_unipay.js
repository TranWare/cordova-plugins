 'use strict';
 
    var IDTech = ( typeof IDTech === 'undefined' ? {} : IDTech );
    var cordova = window.cordova || window.Cordova,
        fail = function(error) {
            console.log('Error running your request: ' + error);
        };
 
        IDTech.fnord = function(callback, error) {
        $('.error').html('Making cordova.exec call to fnord', []);
        var success = function(fnord) {
           	$("#result").html(fnord);
               callback(fnord);
           };
        var fail_handler = error || fail;

        cordova.exec(success, fail_handler, 'com.tranware.UniPay', 'fnord', []);
        $('.error').html('Made cordova.exec call to fnord');
    };


    module.exports = IDTech;