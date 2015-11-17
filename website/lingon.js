#!/usr/bin/env node

var lingon = require('lingon');
var gitDeploy = require('lingon-git-deploy');
var lr = require('lingon-livereload');
var sass = require('gulp-sass');
var autoprefixer = require('gulp-autoprefixer');
var highlight = require('gulp-highlight');
var through = require('through2');

lr(lingon);

lingon.global.title = 'Apollo';
lingon.global.description = 'Apollo';
lingon.global.url = 'https://spotify.github.io/apollo';

lingon.global.apolloVersion = "1.0.1";

// Necessary until this is fixed: https://github.com/johannestroeger/gulp-highlight/pull/5
var unescapeQuot = function() {
  return through.obj(function(file, enc, cont) {
    file.contents = new Buffer(
        file.contents.toString()
            .replace(/&amp;quot;/g, '&quot;')
            .replace(/&amp;gt;/g, '&gt;'));
    this.push(file);
    return cont();
  });
};

lingon.config.directiveFileTypes.push('scss');
lingon.preProcessors.push('scss', function(context, globals) {
  return sass({includePaths: [__dirname]});
});
lingon.postProcessors.push('scss', function() {
  return autoprefixer();
});
lingon.postProcessors.push(['html', 'ejs'], function() {
  return [highlight(), unescapeQuot()];
});

gitDeploy(lingon, {
  remote: 'git@github.com:spotify/apollo.git',
  branch: 'gh-pages'
});
