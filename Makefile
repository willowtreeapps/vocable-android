.PHONY: flake8
flake8:
	@flake8 *.py tcms_junit_plugin tests


.PHONY: pylint
pylint:
	pylint -d missing-docstring *.py tcms_junit_plugin/
	pylint --load-plugins=pylint.extensions.no_self_use \
	    -d missing-docstring -d invalid-name -d too-few-public-methods \
	    -d protected-access -d duplicate-code tests/


.PHONY: test
test:
	nose2 -v --with-coverage --coverage tcms_junit_plugin


.PHONY: junit.xml
junit.xml:
	./make-junit.xml


.PHONY: check-build
check-build:
	./tests/bin/check-build


.PHONY: ci
ci: flake8 pylint test
