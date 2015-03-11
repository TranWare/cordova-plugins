 'use strict';

   var IDTech = ( typeof IDTech === 'undefined' ? {} : IDTech );
   var cordova = window.cordova || window.Cordova,
       IDTech.fnord = function(callback, error) {
       cordova.exec(callback, error, 'com.tranware.UniPay', 'fnord', []);
   };

   module.exports = IDTech;