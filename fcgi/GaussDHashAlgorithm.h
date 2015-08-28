#ifndef GAUSSDHASH_H
#define	GAUSSDHASH_H

#include <opencv2/opencv.hpp>

#include "HashAlgorithm.h"
#include "HashType.h"


class GaussDHashAlgorithm : public HashAlgorithm {
public:
    virtual ~GaussDHashAlgorithm();
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data);
    virtual HashType getType();
    virtual size_t getHashSize();
private:
    virtual cv::Size getKeepRatioSize(const cv::Mat& image, int size);
};

#endif	/* GAUSSDHASH_H */

