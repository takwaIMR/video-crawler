# video-crawler
Crawl websites for videos from Youtube, Vimeo, Soundcloud, etc

I use this to get videos for https://www.findlectures.com, and articles for personalized newsletters (https://www.findlectures.com/form?type=alert).

Prerequisites:
- youtube-dl
- Node.js
- curl
- ffmpeg

If these commands are not on the path, you can set the locations of these:

FFMPEG=d:/Software/ffmpeg-20160619-5f5a97d-win32-static/bin/ffmpeg.exe

YOUTUBE_DL=

NODE=

CURL=

FFMPEG=

If you want to get zip files of talks + transcripts emailed to you, set up a Postmark account and set these:

POSTMARK_API_KEY=

POSTMARK_BCC=

POSTMARK_REPLY_TO=

Examples:
========

I run these through Intellij

*TalkToMarkdown*

_args:_ https://www.youtube.com/watch?v=YME2eyde38A&feature=youtu.be

_environment variables required:_ FFMPEG, YOUTUBE_DL

Use this if you want to convert a Youtube video to a markdown / blog post format.

*MetadataCrawler "test.js"*

Use this if you're crawling a site.
