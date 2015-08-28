#include "GaussDHashAlgorithm.h"

#include <vector>
#include <sstream>
#include <cmath>

#include <opencv2/opencv.hpp>
#include <dhash.h>

#include "DHashException.h"

using namespace std;

GaussDHashAlgorithm::~GaussDHashAlgorithm() {
}

std::vector<uint64_t> GaussDHashAlgorithm::compute(const std::vector<uint8_t>* data) {
    cv::Mat image = cv::imdecode(*data, CV_LOAD_IMAGE_GRAYSCALE);
    cv::resize(image, image, getKeepRatioSize(image, 512), 0, 0, cv::INTER_LANCZOS4);
    
    cv::Mat g1, g2;
    cv::GaussianBlur(image, g1, cv::Size(1, 1), 0);
    cv::GaussianBlur(image, g2, cv::Size(3, 3), 60);
    image = g1 - g2;
    
    std::vector<uint8_t> gaussdata;
    cv::imencode(".png", image, gaussdata);
    
    dhash_err* err = dhash_new_err();
    uint64_t hash = dhash_compute_blob(&gaussdata[0], gaussdata.size(), err);
    
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

HashType GaussDHashAlgorithm::getType() {
    return GaussDHash;
}

size_t GaussDHashAlgorithm::getHashSize() {
    return 8;
}

cv::Size GaussDHashAlgorithm::getKeepRatioSize(const cv::Mat& image, int size) {
    int width = image.cols;
    int height = image.rows;
    
    double ratio;
    
    if (width < height) {
        ratio = size * 1.0 / width;
    } else {
        ratio = size * 1.0 / height;
    }
    return cv::Size((int) round(width*ratio), (int) round(height*ratio));
}