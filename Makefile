BUILDDIR:=build
SRC:=src
JAVAC:=javac
JCFLAGS:=-d $(BUILDDIR) -classpath $(BUILDDIR) -g -sourcepath $(SRC)
SRCDIRS:=$(patsubst src/%, %, $(shell find src -mindepth 1 -type d))
SRCFILES:=$(shell find src -iname '*.java')
BUILDFILES:=$(patsubst %.java,%.class,$(patsubst src/%,build/%,$(SRCFILES)))
args?=test.dot

all: $(BUILDFILES)

define TARGETS
$(BUILDDIR)/$(1)/%.class: $(SRC)/$(1)/%.java Makefile | $(BUILDDIR)
	@$(JAVAC) $(JCFLAGS) $$<
endef

$(foreach dir, $(SRCDIRS), $(eval $(call TARGETS,$(dir))))

$(BUILDDIR):
	@mkdir $(BUILDDIR)

clean:
	rm -rf $(BUILDDIR)

run: $(BUILDFILES)
	@java -classpath $(BUILDDIR) scheduler.Main $(args)
