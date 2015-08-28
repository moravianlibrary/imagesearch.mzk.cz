#ifndef GAUSS2DHASH_H
#define	GAUSS2DHASH_H

#include <opencv2/opencv.hpp>

#include "HashAlgorithm.h"
#include "HashType.h"


class Gauss2DHashAlgorithm : public HashAlgorithm {
public:
    virtual ~Gauss2DHashAlgorithm();
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data);
    virtual HashType getType();
    virtual size_t getHashSize();
private:
    virtual cv::Size getKeepRatioSize(const cv::Mat& image, int size);
};

#endif	/* GAUSS2DHASH_H */

