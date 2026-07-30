"""
File logging for the data received from the Android client.

The assignment asks for "log the received data to a file with a timestamp".
The Python standard library covers this completely, so no extra dependency is
introduced.

Two deliberate choices:

1. A dedicated logger name ("dashboard.sync") instead of the root logger, so
   our log file contains only our own records and not uvicorn's access log.
2. Timestamps are formatted in UTC, matching the UTC timestamps in the API
   responses. Mixed time zones inside one system are a classic source of
   confusion when comparing a log line with an API payload.
"""

import logging
import time
from logging.handlers import TimedRotatingFileHandler
from pathlib import Path

LOGGER_NAME = "dashboard.sync"

# logs/ lives next to the app/ package, i.e. in the project root.
LOG_DIRECTORY = Path(__file__).resolve().parent.parent / "logs"
LOG_FILE = LOG_DIRECTORY / "dashboard.log"


def configure_logging() -> logging.Logger:
    """
    Create (once) and return the logger used for received dashboard data.

    Calling this twice must not attach the handler twice, otherwise every line
    would be written to the file multiple times - hence the `handlers` check.
    """
    logger = logging.getLogger(LOGGER_NAME)

    if logger.handlers:  # already configured (e.g. by a second import or a test)
        return logger

    LOG_DIRECTORY.mkdir(parents=True, exist_ok=True)

    # One file per day; old days are kept as dashboard.log.YYYY-MM-DD.
    handler = TimedRotatingFileHandler(
        LOG_FILE, when="midnight", encoding="utf-8", utc=True
    )

    # A custom `datefmt` drops the milliseconds that logging adds by default,
    # so they are appended explicitly. At one client update per second a
    # second-only timestamp would not be precise enough to order the entries.
    # The trailing "Z" documents that the timestamp is UTC.
    formatter = logging.Formatter(
        fmt="%(asctime)s.%(msecs)03dZ | %(levelname)s | %(message)s",
        datefmt="%Y-%m-%dT%H:%M:%S",
    )
    formatter.converter = time.gmtime  # write timestamps in UTC, not local time
    handler.setFormatter(formatter)

    logger.addHandler(handler)
    logger.setLevel(logging.INFO)
    logger.propagate = False  # keep these records out of the console/root logger

    return logger
