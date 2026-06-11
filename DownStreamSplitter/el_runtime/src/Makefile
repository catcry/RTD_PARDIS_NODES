
#
# Makefile for building node applications
#
# (c) Comptel Corporation 2016
#

# List the C sources here
C_SOURCES = actions_splitter.c

# List the C++ sources here
#C++_SOURCES = cpp-source.cc
# Application name, must start with "app_"
APP =app_rtd_action_splitter.so

# Build a debug version of the application
#PROJECTSTATE = debug

# Path to the directory containing EventLink SDK libraries and headers
INCLUDEPATH = -I/comptel/elink/installation/EventLink/sdk/include
LIBPATH = -L/comptel/elink/installation/EventLink/sdk/lib


##################################################
#
# No editable parameters below
#
##################################################

# list of generated object files.
COBJ = $(C_SOURCES:.c=.o)
C++OBJ = $(C++_SOURCES:.cc=.o)
OBJS = $(COBJ) $(C++OBJ)

# Compiler, linker and flags
CC=/usr/bin/gcc
CPP=CC
LD=/usr/bin/ld
CFLAGS= -Wall -fPIC $(INCLUDEPATH)
CPPFLAGS= -Wall -fPIC $(INCLUDEPATH)
LDFLAGS= -shared $(LIBPATH) -lnodebase

# top-level rule, to compile everything.
all: $(APP)

clean:
	@echo Cleaning up.
	@rm -f *.o *~ $(APP)

# Rules to compile sources
%.o: %.c
	@echo Compiling $<
	@if [ "$(PROJECTSTATE)x" = "debugx" ]; then \
	$(CC) $(CFLAGS) -g -c $< ;\
	else \
	$(CC) $(CFLAGS) -O -c $< ;\
	fi

%.o: %.cc
	@echo Compiling $<
	@if [ "$(PROJECTSTATE)x" = "debugx" ]; then \
	$(CPP) $(CPPFLAGS) -g -c $< ;\
	else \
	$(CPP) $(CPPFLAGS) -O -c $< ;\
	fi

# Rule to link the application
$(APP): $(OBJS) 
	@echo Linking $(APP)
	@$(LD) $(LDFLAGS) $(OBJS) -o $(APP)
	@mv $(APP) ../cfg/AnyOS
	@echo Done.
