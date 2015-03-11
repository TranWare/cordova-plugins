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

     //  cordova.exec(callback, error, 'com.tranware.UniPay', 'fnord', []);
   };


   module.exports = IDTech;