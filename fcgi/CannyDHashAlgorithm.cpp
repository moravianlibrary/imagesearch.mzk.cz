/* 
 * File:   CannyDHash.cpp
 * Author: dudae
 * 
 * Created on Štvrtok, 2015, júla 9, 10:12
 */

#include "CannyDHashAlgorithm.h"

#include <vector>
#include <sstream>

#include <opencv2/opencv.hpp>
#include <dhash.h>

#include "DHashException.h"

using namespace std;

const int low_threshold = 50;
const int ratio = 3;
const int kernel_size = 3;

CannyDHashAlgorithm::~CannyDHashAlgorithm() {
}

std::vector<uint64_t> CannyDHashAlgorithm::compute(const std::vector<uint8_t>* data) {
    cv::Mat image = cv::imdecode(*data, CV_LOAD_IMAGE_GRAYSCALE);
    cv::blur(image, image, cv::Size(3, 3));
    
    cv::Canny(image, image, low_threshold, low_threshold*ratio, kernel_size);
    
    std::vector<uint8_t> cannydata;
    cv::imencode(".png", image, cannydata);
    
    dhash_err* err = dhash_new_err();
    uint64_t hash = dhash_compute_blob(&cannydata[0], cannydata.size(), err);
    
    if (err->err_type != OK) {
        ostringstream os;
        os << "Error occured during compute dhash: " << err->description;
        dhash_free_err(err);
        throw DHashException(os.str());
    }
    dhash_free_err(err);
    
    vector<uint64_t> result;
    result.push_back(hash);
    return result;
}

HashType CannyDHashAlgorithm::getType() {
    return CannyDHash;
}

size_t CannyDHashAlgorithm::getHashSize() {
    return 32;
}
