#!/usr/bin/env bash
NUX_PACKAGES=${NUXEO_PACKAGES:-nuxeo-web-ui nuxeo-platform-importer}

if [[ -e ${NUXEO_HOME}/packages/configured-pkg ]]; then
  echo "# Refresh MP from cache"
  rm -rf /opt/nuxeo/server/packages/.packages
  NUXEO_PACKAGES="" /docker-entrypoint.sh nuxeoctl mp-install --relax=true --accept=true  /opt/nuxeo/server/packages/cache/*
else
  start=$(date --utc +%Y%m%d_%H%M%SZ)
  echo "# ${start}: Installing MP ${NUX_PACKAGES} ..."
  NUXEO_PACKAGES="" /docker-entrypoint.sh nuxeoctl mp-install --relax=true --accept=true $NUX_PACKAGES 
  touch ${NUXEO_HOME}/packages/configured-pkg
  # use a cache because we cannot reinstall package from the store directory
  cp -ar /opt/nuxeo/server/packages/store/ /opt/nuxeo/server/packages/cache
  end=$(date --utc +%Y%m%d_%H%M%SZ)
  echo "# ${end}: MP installed"
fi
now=$(date --utc +%Y%m%d_%H%M%SZ)
cp ${NUXEO_CONF} "/var/lib/nuxeo/nuxeo-${now}.conf"
exec /docker-entrypoint.sh "$@"
