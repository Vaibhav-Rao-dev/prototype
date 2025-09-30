(function(){
  angular.module('masApp')
    .factory('ApiService', ['$http', function($http){
      var base = '';
      return {
        get: function(path){ return $http.get(base + path); },
        post: function(path, body){ return $http.post(base + path, body); }
      };
    }]);
})();
