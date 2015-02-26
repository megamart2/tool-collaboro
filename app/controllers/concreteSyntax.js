angular.module('collaboroControllers').controller('ConcreteSyntaxController', ['$scope', '$http', 'syntaxService',
  function($scope, $http, syntax) {
    $scope.concreteSyntaxImage = "assets/img/loading.gif";
    $scope.totalConcreteSyntaxImages = 1;
    $scope.currentConcreteSyntaxIndex = -1;

    $scope.updateTotalNumber = function() {
      syntax.getTotalConcreteSyntaxImages(
        function(response) {
          $scope.totalConcreteSyntaxImages = response.data.numImages;
        }
      );
    };

    $scope.updateTotalNumber();

    $scope.$on('dslVersionChanged', function() {
      $scope.concreteSyntaxImage = "assets/img/loading.gif";
      $scope.currentConcreteSyntaxIndex = -1;
      $scope.updateTotalNumber();
      $scope.nextConcreteSyntaxImage();
    });

    $scope.nextConcreteSyntaxImage = function() {
      var nextImageIndex = 0;
      if($scope.currentConcreteSyntaxIndex + 1 < $scope.totalConcreteSyntaxImages) {
        nextImageIndex = $scope.currentConcreteSyntaxIndex + 1;
        $scope.concreteSyntaxImage="assets/img/loading.gif";
        syntax.getConcreteSyntaxImage(nextImageIndex,
          function(response) {
            $scope.concreteSyntaxImage = "data:image/jpg;base64," + response.data;
            $scope.currentConcreteSyntaxIndex = nextImageIndex;
          }
        );
      }

    };

    $scope.nextConcreteSyntaxImage();

    $scope.previousConcreteSyntaxImage = function() {
      var previousImageIndex = $scope.totalConcreteSyntaxImages - 1;
      if($scope.currentConcreteSyntaxIndex - 1 >= 0) {
        previousImageIndex = $scope.currentConcreteSyntaxIndex - 1;
        $scope.concreteSyntaxImage="assets/img/loading.gif";
        syntax.getConcreteSyntaxImage(previousImageIndex,
          function(response) {
            $scope.concreteSyntaxImage = "data:image/jpg;base64," + response.data;
            $scope.currentConcreteSyntaxIndex = previousImageIndex;
          }
        );
      }
    };
}]);