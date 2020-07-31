package com.newrelic.agent.xml;

public enum XmlInstrumentOptions {

    /**
     * The path to the custom extension xml file.
     */
    FILE_PATH("file", true, "The full path to the xml extension file to be validated. This must be set.") {
        @Override
        public void validateAndAddParameter(XmlInstrumentParams pInstrument, String[] pValue, String pTagName) {
            pInstrument.setFile(pValue, pTagName);
        }
    },
    /**
     * True means debug will be on and more information will be outputed to the command line.
     */
    DEBUG_FLAG("debug", true, "Set this flag to true for more debuging information. The default is false.") {
        @Override
        public void validateAndAddParameter(XmlInstrumentParams pInstrument, String[] pValue, String pTagName) {
            pInstrument.setDebug(pValue, pTagName);

        }
    };

    /**
     * The flag name to use on the command line.
     */
    private final String flagName;

    /**
     * True means the property requires an argument after the flag.
     */
    private boolean argRequired;

    /** The description to be printed out to the command line. */
    private final String description;

    /**
     * 
     * Creates this InstrumentOptions.
     * 
     * @param pFlagName The flag name to use on the command line.
     * @param pRequired True means the property requires an argument after the flag.
     * @param pDescription The description to print out.
     */
    XmlInstrumentOptions(final String pFlagName, final boolean pRequired, final String pDescription) {
        flagName = pFlagName;
        argRequired = pRequired;
        description = pDescription;
    }

    /**
     * Takes the input value and sets it on the instrument.
     * 
     * @param instrument Where the values from the command line should be stored.
     * @param value The current value.
     * @param tagName The name to use in exceptions.
     */
    public abstract void validateAndAddParameter(final XmlInstrumentParams instrument, final String[] value,
            String tagName);

    /**
     * Gets the field flagName.
     * 
     * @return the flagName
     */
    public String getFlagName() {
        return flagName;
    }

    /**
     * Gets the field required.
     * 
     * @return the required
     */
    public boolean isArgRequired() {
        return argRequired;
    }

    /**
     * Gets the field description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
