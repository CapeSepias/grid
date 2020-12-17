import angular from 'angular';
import { trackAll } from '../util/batch-tracking';
// import PQueue from "p-queue";

var labelService = angular.module('kahuna.services.label', []);

labelService.factory('labelService',
                     ['$rootScope', '$q', 'apiPoll', 'imageAccessor','$timeout',
                       // eslint-disable-next-line no-unused-vars
                       function ($rootScope, $q, apiPoll, imageAccessor, $timeout) {


    function readLabelName(label) {
        return label.data;
    }

    function readLabelsName(labels) {
        return labels.map(readLabelName);
    }

    function labelsEquals(labelsA, labelsB) {
        return angular.equals(
            readLabelsName(labelsA).sort(),
            readLabelsName(labelsB).sort()
        );
    }
                        function untilLabelsEqual(image, expectedLabels) {
        return image.get().then(apiImage => {
            const apiLabels = imageAccessor.readLabels(apiImage);
            if (labelsEquals(apiLabels, expectedLabels)) {
                return apiImage;
            } else {
                return $q.reject();
            }
        });
    }

    function remove (image, label) {
        var existingLabels = imageAccessor.readLabels(image);
        var labelIndex = existingLabels.findIndex(lbl => lbl.data === label);
        if (labelIndex !== -1) {
            return existingLabels[labelIndex]
                .delete()
                .then(newLabels => apiPoll(() => untilLabelsEqual(image, newLabels.data)))
                .then(newImage => {
                    $rootScope.$emit('image-updated', newImage, image);
                    return newImage;
                });
        }

        // no-op
        return Promise.resolve(image);
    }

    function add (image, labels) {
        labels = labels.filter(label => label && label.trim().length > 0);

      return image.data.userMetadata.data.labels
        .post({ data: labels })
          .then(newLabels => {
            console.log(labels, newLabels);
           return  apiPoll(() => untilLabelsEqual(image, newLabels.data));
          })
            .then(newImage => {
                $rootScope.$emit('image-updated', newImage, image);
              return image;
            });
    }



                        function batchAdd(images, labels) {
                          const sendAdd = (image) => {
                            labels = labels.filter(label => label && label.trim().length > 0);

                            return image.data.userMetadata.data.labels
                              .post({ data: labels });
                          };
                          const checkAdd = (image, result) => {
                            return apiPoll(() => untilLabelsEqual(image, result.data));
                          };
                          // const queue = new PQueue({ concurrency: 1 });

                          // const updateGrid = async (image, newImage) => queue.add(()=>new Promise(resolve => {
                          //   console.log(image, 'LOOK AT maxHeight: ');
                          //   $timeout(() => {
                          //     $rootScope.$emit(
                          //       "image-updated",
                          //       newImage,
                          //       image
                          //     );
                          //     $timeout(() => {
                          //       resolve();
                          //     }, 60);
                          //   },60);
                          //   return newImage;
                          // }));
                          console.log(checkAdd);
                          return trackAll($rootScope, "label", images, sendAdd, checkAdd);//, updateGrid);
    }

    function batchRemove (images, label) {
        const affectedImages = images.filter(image =>
            imageAccessor.readLabels(image).some(({ data }) => data === label)
        );

        return trackAll($rootScope, "label", affectedImages, image => remove(image, label));
    }

    return {
        add,
        remove,
        batchAdd,
        batchRemove
    };
}]);

export default labelService;
