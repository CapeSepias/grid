import angular from 'angular';

import template from './gr-export-original-image.html';

export const exportOriginalImage = angular.module('gr.exportOriginalImage', []);

exportOriginalImage.controller('grExportOriginalImageCtrl', [
    '$scope', '$rootScope', '$state', '$stateParams', 'mediaCropper', 'pollUntilCropCreated',
    function($scope, $rootScope, $state, $stateParams, mediaCropper, pollUntilCropCreated) {
        let ctrl = this;
        const imageId = $stateParams.imageId;

        ctrl.callCrop = function() {
            //prevents return key on the crop button posting crop twice
            if (!ctrl.cropping) {
                crop();
            }
        };

        function crop() {
            ctrl.cropping = true;

            mediaCropper.createFullCrop(ctrl.image).then(crop => {
                //Global notification of action
                $rootScope.$emit('events:crop-created', {
                    image: ctrl.image,
                    crop: crop
                });

                return pollUntilCropCreated(ctrl.image, crop.data.id)
                  .then(() => {
                      $state.go('image', {
                          imageId,
                          crop: crop.data.id
                      });
                  });
            }).finally(() => {
                ctrl.cropping = false;
            });
        }
    }
]);

exportOriginalImage.directive('grExportOriginalImage', [function() {
    return {
        restrict: 'E',
        controller: 'grExportOriginalImageCtrl',
        controllerAs: 'ctrl',
        bindToController: true,
        scope: {
            image: '=',
            trackingLocation: '@',
            cropping: '='
        },
        template: template
    };
}]);
