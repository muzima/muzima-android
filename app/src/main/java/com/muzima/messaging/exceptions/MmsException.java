package com.muzima.messaging.exceptions;

public class MmsException extends Exception {
        private static final long serialVersionUID = -7323249827281485390L;

        /**
         * Creates a new MmsException.
         */
        public MmsException() {
            super();
        }

        /**
         * Creates a new MmsException with the specified detail message.
         *
         * @param message the detail message.
         */
        public MmsException(String message) {
            super(message);
        }

        /**
         * Creates a new MmsException with the specified cause.
         *
         * @param cause the cause.
         */
        public MmsException(Throwable cause) {
            super(cause);
        }

        /**
         * Creates a new MmsException with the specified detail message and cause.
         *
         * @param message the detail message.
         * @param cause the cause.
         */
        public MmsException(String message, Throwable cause) {
            super(message, cause);
        }
}
