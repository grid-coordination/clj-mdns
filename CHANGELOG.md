# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.1.1] - 2026-04-27
### Added
- Embed git build provenance in the JAR via `com.dcj/build-provenance` 0.2.0. Each JAR now carries `META-INF/energy.grid-coordination/clj-mdns/build-provenance.{edn,json}` with `:artifact :version :commit :commit-abbreviated :branch :describe :dirty`.
- README badges for `md-docs` and `build-provenance` conventions.

### Changed
- Bump `com.dcj/codox-md` build-time dep to 0.2.0.

## [0.1.0] - 2026-04
### Added
- Initial release. Clojure wrapper around jmdns for mDNS/DNS-SD service discovery.

[Unreleased]: https://github.com/grid-coordination/clj-mdns/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/grid-coordination/clj-mdns/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/grid-coordination/clj-mdns/releases/tag/v0.1.0
