#include "GaussBlockHashAlgorithm.h"

#include <vector>
#include <sstream>
#include <cmath>

#include <opencv2/opencv.hpp>
#include <blockhash.h>

using namespace std;

GaussBlockHashAlgorithm::~GaussBlockHashAlgorithm() {
}

std::vector<uint64_t> GaussBlockHashAlgorithm::compute(const std::vector<uint8_t>* data) {
    cv::Mat image = cv::imdecode(*data, CV_LOAD_IMAGE_GRAYSCALE);
    cv::resize(image, image, getKeepRatioSize(image, 512), 0, 0, cv::INTER_LANCZOS4);
    
    cv::Mat g1, g2;
    cv::GaussianBlur(image, g1, cv::Size(1, 1), 0);
    cv::GaussianBlur(image, g2, cv::Size(3, 3), 60);
    image = g1 - g2;
    
    std::vector<uint8_t> gaussdata;
    cv::imencode(".png", image, gaussdata);
    
    return blockhash::compute(gaussdata, 256);
}

HashType GaussBlockHashAlgorithm::getType() {
    return GaussBlockHash;
}

size_t GaussBlockHashAlgorithm::getHashSize() {
    return 32;
}

cv::Size GaussBlockHashAlgorithm::getKeepRatioSize(const cv::Mat& image, int size) {
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