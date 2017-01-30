#!/bin/sh
lein do clean, cljsbuild once dist
git subtree push --prefix build origin gh-pages
