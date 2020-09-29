import angular from 'angular';

const CROP_TYPE_STORAGE_KEY = 'cropType';
const CUSTOM_CROP_STORAGE_KEY = 'customCrop';

// `ratioString` is sent to the server, being `undefined` for `freeform` is expected 🙈
const landscape = {key: 'landscape', ratio: 5 / 3, ratioString: '5:3'};
const portrait = {key: 'portrait', ratio: 4 / 5, ratioString: '4:5'};
const video = {key: 'video', ratio: 16 / 9, ratioString: '16:9'};
const square = {key: 'square', ratio: 1, ratioString: '1:1'};
const freeform = {key: 'freeform', ratio: null};

const customCrop = (label, xRatio, yRatio) => {
  return { key:label, ratio: xRatio / yRatio, ratioString: `${xRatio}:${yRatio}`};
};

const cropOptions = [landscape, portrait, video, square, freeform];

export const cropUtil = angular.module('util.crop', ['util.storage']);

cropUtil.constant('landscape', landscape);
cropUtil.constant('portrait', portrait);
cropUtil.constant('video', video);
cropUtil.constant('square', square);
cropUtil.constant('freeform', freeform);
cropUtil.constant('cropOptions', cropOptions);
cropUtil.constant('defaultCrop', landscape);

cropUtil.factory('cropSettings', ['storage', function(storage) {
  const isValidCropType = cropType => cropOptions.some(_ => _.key === cropType);

  const isValidRatio = ratio => {
    const splitRatio = ratio.split(',');
    return !(splitRatio.length < 3 || isNaN(splitRatio[1]) || isNaN(splitRatio[2]));
  };

  const parseRatio = ratio => {
    // example ratio 'longcrop,1,5'
    if (isValidRatio(ratio)) {
      const splitRatio = ratio.split(',');
      return {
        label: splitRatio[0],
        x: parseInt(splitRatio[1], 10),
        y: parseInt(splitRatio[2], 10)
      };
    }
  };

  const setCropType = (cropType) => {
    if (isValidCropType(cropType)) {
      storage.setJs(CROP_TYPE_STORAGE_KEY, cropType, true);
    } else {
      storage.clearJs(CROP_TYPE_STORAGE_KEY);
    }
  };

  const setCustomCrop = customRatio => {
    const parsedRatio = parseRatio(customRatio);
    if (parsedRatio) {
      storage.setJs(CUSTOM_CROP_STORAGE_KEY, customCrop(parsedRatio.label, parsedRatio.x, parsedRatio.y), true);
    } else {
      storage.clearJs(CUSTOM_CROP_STORAGE_KEY);
    }
  };

  function set({cropType, customRatio}) {
    if (cropType) {
      setCropType(cropType);
    }

    if (customRatio) {
      setCustomCrop(customRatio);
    }
  }

  function getCropOptions() {
    const customCrop =  storage.getJs(CUSTOM_CROP_STORAGE_KEY, true);
    return customCrop ? cropOptions.concat(customCrop) : cropOptions;
  }

  function getCropType() {
    const cropType = storage.getJs(CROP_TYPE_STORAGE_KEY, true);

    if (isValidCropType(cropType)) {
      return cropType;
    }
  }

  return { set, getCropType, getCropOptions };
}]);

cropUtil.filter('asCropType', function() {
  return ratioString => {
    const cropSpec = cropOptions.find(_ => _.ratioString === ratioString) || freeform;
    return cropSpec.key;
  };
});
