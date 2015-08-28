#ifndef GAUSSBLOCKHASH_H
#define	GAUSSBLOCKHASH_H

#include <opencv2/opencv.hpp>

#include "HashAlgorithm.h"
#include "HashType.h"


class GaussBlockHashAlgorithm : public HashAlgorithm {
public:
    virtual ~GaussBlockHashAlgorithm();
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data);
    virtual HashType getType();
    virtual size_t getHashSize();
private:
    virtual cv::Size getKeepRatioSize(const cv::Mat& image, int size);
};

#endif	/* GAUSSBLOCKHASH_H */

