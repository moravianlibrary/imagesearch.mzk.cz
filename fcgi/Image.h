/* 
 * File:   Image.h
 * Author: dudae
 *
 * Created on Streda, 2015, júla 8, 8:33
 */

#ifndef IMAGE_H
#define	IMAGE_H

#include <stdint.h>
#include <vector>
#include <string>

class Image {
public:
    static std::vector<uint8_t>* fromUrl(const std::string& url);
    
    static std::vector<uint8_t>* fromBase64(const std::string& base64);
};

#endif	/* IMAGE_H */

