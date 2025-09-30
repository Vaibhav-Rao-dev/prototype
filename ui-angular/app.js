(function(){
  angular.module('masApp', ['ngRoute'])
    .config(['$routeProvider', function($routeProvider){
      $routeProvider
        .when('/', {templateUrl: 'hunter.html', controller: 'HunterCtrl'})
        .when('/hunter', {templateUrl: 'hunter.html', controller: 'HunterCtrl'})
  .when('/guardian', {templateUrl: 'guardian.html', controller: 'GuardianCtrl'})
  .when('/analyst', {templateUrl: 'analyst.html'})
  .when('/phalanx', {templateUrl: 'phalanx.html'})
  .when('/warden', {templateUrl: 'warden.html'})
  .when('/stratus', {templateUrl: 'stratus.html'})
  .when('/sentinel', {templateUrl: 'sentinel.html'})
  .when('/orchestrator', {templateUrl: 'orchestrator.html'})
        .otherwise({redirectTo:'/'});
    }])
    .factory('Api', ['$http', function($http){
      var base = '';
      // mock-mode when backend unavailable; override this flag in console or UI
      var mock = false;
      var mockCases = [
        { id: 'c-1', title: 'Brute force on gateway', summary: 'Detected >50 failed logins', riskScore: 85, timeline: [{time:'now', detail:'failed logins'}] },
        { id: 'c-2', title: 'Suspicious process', summary: 'Unknown binary executed', riskScore: 45, timeline: [{time:'now', detail:'process spawn'}] }
      ];
      // richer mock fixtures
      var mockPlaybooks = [{id:'pb-1', name:'Containment playbook'},{id:'pb-2', name:'Patch and verify'}];
      var mockRuns = [{id:'run-1', playbookId:'pb-1', status:'completed'},{id:'run-2', playbookId:'pb-2', status:'running'}];

      return {
        setMock: function(v){ mock = !!v; },
        isMock: function(){ return mock; },
        // generic helpers
        get: function(path){ return mock ? Promise.resolve({data:[]}) : $http.get(base + path); },
        post: function(path, body){ return mock ? Promise.resolve({data:{status:'mocked'}}) : $http.post(base + path, body); },
        listCases: function(){ return mock ? Promise.resolve({data: mockCases}) : $http.get(base + '/cases'); },
        getCase: function(id){
          if (mock) { var m = mockCases.find(c=>c.id===id); return Promise.resolve({data: m||null}); }
          return $http.get(base + '/cases/' + id);
        },
        publishEvent: function(evt){
          if (mock) { mockCases.unshift({ id: 'c-'+(mockCases.length+1), title: 'Mock hunt: '+(evt.query||evt.type), summary: 'Mock generated', riskScore: 30, timeline:[] }); return Promise.resolve({data:{status:'mocked'}}); }
          // backend publish endpoint is /publish
          return $http.post(base + '/publish', evt);
        },
        actionCase: function(id, action, body){ return mock ? Promise.resolve({data:{status:'mocked'}}) : $http.post(base + '/cases/' + id + '/action', {action:action, body: body}); },
        listPlaybooks: function(){ return mock ? Promise.resolve({data: mockPlaybooks}) : $http.get(base + '/playbooks'); },
        runPlaybook: function(playbookId, target){ return mock ? Promise.resolve({data:{runId:'mock-run-'+Math.floor(Math.random()*1000)}}) : $http.post(base + '/playbooks/' + playbookId + '/run', {target: target}); },
        hunterLandscape: function(){ return mock ? Promise.resolve({data:{assets:[],links:[]}}) : $http.get(base + '/hunter/landscape'); },
        hunterCases: function(){ return mock ? Promise.resolve({data: mockCases}) : $http.get(base + '/hunter/cases'); },
        hunterSearch: function(q){ return mock ? Promise.resolve({data:{status:'mocked',query:q}}) : $http.post(base + '/hunter/search', {nl:q}); }
      };
    }])
  .controller('HunterCtrl', ['$scope','Api','$timeout', function($scope, Api, $timeout){
      $scope.cases = [];
      $scope.assets = [];
      $scope.links = [];
      $scope.selectedCase = null;
      $scope.playbooks = [];
      $scope.mockMode = false;

      $scope.toggleMock = function(){
        $scope.mockMode = !$scope.mockMode;
        Api.setMock($scope.mockMode);
        loadCases();
        loadLandscape();
      };

      function loadCases(){
        Api.listCases().then(function(res){ $scope.cases = res.data || []; });
      }

      function loadLandscape(){
        Api.hunterLandscape().then(function(res){ var d = res.data || {}; $scope.assets = d.assets || []; $scope.links = d.links || []; });
      }

      function loadPlaybooks(){
        Api.listPlaybooks().then(function(res){ $scope.playbooks = res.data; });
      }

      $scope.openCase = function(c){
        Api.getCase(c.id).then(function(res){ $scope.selectedCase = res.data; });
      };

      $scope.action = function(c, action){
        Api.actionCase(c.id, action).then(function(){ loadCases(); });
      };

      $scope.executePlaybook = function(c, playbookId){
        Api.runPlaybook(playbookId, {caseId: c.id}).then(function(res){ alert('Playbook run: ' + res.data.runId); });
      };

      $scope.nlSearch = function(){
        var q = $scope.nlQuery || '';
        Api.hunterSearch(q).then(function(res){ $timeout(loadCases, 800); });
      };

      // initial load
  loadCases();
  loadPlaybooks();
  loadLandscape();

      // demo assets (static layout)
      $scope.assets = [
        {id: 'srv-1', x: 60, y: 40, status: 'normal'},
        {id: 'db-1', x: 240, y: 40, status: 'normal'},
        {id: 'fw-1', x: 150, y: 160, status: 'anomalous'}
      ];
      $scope.links = [
        {x1:72,y1:40,x2:228,y2:40,anomaly:false},
        {x1:150,y1:52,x2:150,y2:148,anomaly:true}
      ];

    }]);

    angular.module('masApp').controller('GuardianCtrl', ['$scope','Api', function($scope, Api){
      $scope.vulns = [];
      $scope.scheduling = false;
      $scope.remediate = function(v){
        Api.post('/guardian/vulns/' + v.id + '/remediate', {action: 'remediate'}).then(function(){ loadVulns(); });
      };
      $scope.schedulePatch = function(){
        $scope.scheduling = true;
        var when = $scope.scheduleWhen || new Date().toISOString();
        var targets = $scope.scheduleTargets || [];
        Api.post('/guardian/patches/schedule', {when: when, targets: targets}).then(function(res){ $scope.scheduling = false; alert('Scheduled: ' + (res.data.ticketId || 'unknown')); }).catch(function(){ $scope.scheduling = false; });
      };
      function loadVulns(){ Api.get('/guardian/vulns').then(function(res){ $scope.vulns = res.data || []; }, function(){ $scope.vulns = []; }); }
      loadVulns();
    }]);
})();
