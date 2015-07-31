#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=GNU-Linux-x86
CND_DLIB_EXT=so
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/BlockHashAlgorithm.o \
	${OBJECTDIR}/CannyDHashAlgorithm.o \
	${OBJECTDIR}/Controller.o \
	${OBJECTDIR}/DHashAlgorithm.o \
	${OBJECTDIR}/HashAlgorithmManager.o \
	${OBJECTDIR}/HashManager.o \
	${OBJECTDIR}/Image.o \
	${OBJECTDIR}/MemoryManager.o \
	${OBJECTDIR}/OrderedList.o \
	${OBJECTDIR}/Record.o \
	${OBJECTDIR}/RecordManager.o \
	${OBJECTDIR}/appio.o \
	${OBJECTDIR}/main.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=-lfcgi++ -lfcgi -lPocoJSON -lPocoDataSQLite -lPocoData -lblockhash -ldhash -lopencv_calib3d -lopencv_contrib -lopencv_core -lopencv_features2d -lopencv_flann -lopencv_highgui -lopencv_imgproc -lopencv_legacy -lopencv_ml -lopencv_objdetect -lopencv_ocl -lopencv_photo -lopencv_stitching -lopencv_superres -lopencv_ts -lopencv_video -lopencv_videostab -lPocoFoundationd -lPocoNet -lPocoNetSSL

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/fcgi

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/fcgi: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/fcgi ${OBJECTFILES} ${LDLIBSOPTIONS} `pkg-config MagickWand --libs`

${OBJECTDIR}/BlockHashAlgorithm.o: BlockHashAlgorithm.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/BlockHashAlgorithm.o BlockHashAlgorithm.cpp

${OBJECTDIR}/CannyDHashAlgorithm.o: CannyDHashAlgorithm.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/CannyDHashAlgorithm.o CannyDHashAlgorithm.cpp

${OBJECTDIR}/Controller.o: Controller.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Controller.o Controller.cpp

${OBJECTDIR}/DHashAlgorithm.o: DHashAlgorithm.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/DHashAlgorithm.o DHashAlgorithm.cpp

${OBJECTDIR}/HashAlgorithmManager.o: HashAlgorithmManager.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/HashAlgorithmManager.o HashAlgorithmManager.cpp

${OBJECTDIR}/HashManager.o: HashManager.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/HashManager.o HashManager.cpp

${OBJECTDIR}/Image.o: Image.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Image.o Image.cpp

${OBJECTDIR}/MemoryManager.o: MemoryManager.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/MemoryManager.o MemoryManager.cpp

${OBJECTDIR}/OrderedList.o: OrderedList.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/OrderedList.o OrderedList.cpp

${OBJECTDIR}/Record.o: Record.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/Record.o Record.cpp

${OBJECTDIR}/RecordManager.o: RecordManager.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/RecordManager.o RecordManager.cpp

${OBJECTDIR}/appio.o: appio.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/appio.o appio.cpp

${OBJECTDIR}/main.o: main.cpp 
	${MKDIR} -p ${OBJECTDIR}
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -Wall -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/main.o main.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/fcgi

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
