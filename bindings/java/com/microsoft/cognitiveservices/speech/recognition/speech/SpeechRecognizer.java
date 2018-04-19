package com.microsoft.cognitiveservices.speech.recognition.speech;
//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//

import java.io.IOException;

import com.microsoft.cognitiveservices.speech.ParameterCollection;
import com.microsoft.cognitiveservices.speech.recognition.ParameterNames;
import com.microsoft.cognitiveservices.speech.recognition.RecognitionErrorEventArgs;
import com.microsoft.cognitiveservices.speech.recognition.translation.TranslationTextResult;
import com.microsoft.cognitiveservices.speech.util.EventHandler;
import com.microsoft.cognitiveservices.speech.util.EventHandlerImpl;
import com.microsoft.cognitiveservices.speech.util.Task;
import com.microsoft.cognitiveservices.speech.util.TaskRunner;

/// <summary>
/// Performs speech recognition from microphone, file, or other audio input streams, and gets transcribed text as result.
/// </summary>
/// <example>
/// An example to use the speech recognizer on a audio file and listen to events generated by the recognizer.
/// <code>
/// static void MySessionEventHandler(object sender, SessionEventArgs e)
/// {
///    Console.WriteLine(String.Format("Speech recognition: Session event: {0} ", e.ToString()));
/// }
///
/// static void MyIntermediateResultEventHandler(object sender, SpeechRecognitionResultEventArgs e)
/// {
///    Console.WriteLine(String.Format("Speech recognition: Intermediate result: {0} ", e.ToString()));
/// }
///
/// static void MyFinalResultEventHandler(object sender, SpeechRecognitionResultEventArgs e)
/// {
///    Console.WriteLine(String.Format("Speech recognition: Final result: {0} ", e.ToString()));
/// }
///
/// static void MyErrorHandler(object sender, RecognitionErrorEventArgs e)
/// {
///    Console.WriteLine(String.Format("Speech recognition: Error information: {0} ", e.ToString()));
/// }
///
/// static void SpeechRecognizerSample()
/// {
///   SpeechRecognizer reco = factory.CreateSpeechRecognizer("audioFileName");
///
///   reco.OnSessionEvent += MySessionEventHandler;
///   reco.FinalResultReceived += MyFinalResultEventHandler;
///   reco.IntermediateResultReceived += MyIntermediateResultEventHandler;
///   reco.RecognitionErrorRaised += MyErrorHandler;
///
///   // Starts recognition.
///   var result = await reco.RecognizeAsync();
///
///   reco.OnSessionEvent -= MySessionEventHandler;
///   reco.FinalResultReceived -= MyFinalResultEventHandler;
///   reco.IntermediateResultReceived -= MyIntermediateResultEventHandler;
///   reco.RecognitionErrorRaised -= MyErrorHandler;
///
///   Console.WriteLine("Speech Recognition: Recognition result: " + result);
/// }
/// </code>
/// </example>
public final class SpeechRecognizer extends com.microsoft.cognitiveservices.speech.recognition.Recognizer
{
    /// <summary>
    /// The event <see cref="IntermediateResultReceived"/> signals that an intermediate recognition result is received.
    /// </summary>
    final public EventHandlerImpl<SpeechRecognitionResultEventArgs> IntermediateResultReceived = new EventHandlerImpl<SpeechRecognitionResultEventArgs>();

    /// <summary>
    /// The event <see cref="FinalResultReceived"/> signals that a final recognition result is received.
    /// </summary>
    final public EventHandlerImpl<SpeechRecognitionResultEventArgs> FinalResultReceived = new EventHandlerImpl<SpeechRecognitionResultEventArgs>();

    /// <summary>
    /// The event <see cref="RecognitionErrorRaised"/> signals that an error occurred during recognition.
    /// </summary>
    final public EventHandlerImpl<RecognitionErrorEventArgs> RecognitionErrorRaised = new EventHandlerImpl<RecognitionErrorEventArgs>();

    public SpeechRecognizer(com.microsoft.cognitiveservices.speech.internal.SpeechRecognizer recoImpl) throws UnsupportedOperationException
    {
        this.recoImpl = recoImpl;

        intermediateResultHandler = new ResultHandlerImpl(this, /*isFinalResultHandler:*/ false);
        recoImpl.getIntermediateResult().addEventListener(intermediateResultHandler);

        finalResultHandler = new ResultHandlerImpl(this, /*isFinalResultHandler:*/ true);
        recoImpl.getFinalResult().addEventListener(finalResultHandler);

        errorHandler = new ErrorHandlerImpl(this);
        recoImpl.getNoMatch().addEventListener(errorHandler);
        recoImpl.getCanceled().addEventListener(errorHandler);

        recoImpl.getSessionStarted().addEventListener(sessionStartedHandler);
        recoImpl.getSessionStopped().addEventListener(sessionStoppedHandler);
        recoImpl.getSpeechStartDetected().addEventListener(speechStartDetectedHandler);
        recoImpl.getSpeechEndDetected().addEventListener(speechEndDetectedHandler);

        _Parameters = new ParameterCollection<SpeechRecognizer>(this);
    }

    /// <summary>
    /// Gets/gets the deployment id of a customized speech model that is used for speech recognition.
    /// </summary>
    public String getDeploymentId()
    {
        return _Parameters.getString(ParameterNames.SpeechModelId);
    }
    
    public void setDeploymentId(String value)
    {
        _Parameters.set(ParameterNames.SpeechModelId, value);
    }

    /// <summary>
    /// Gets/sets the spoken language of recognition.
    /// </summary>
    public String getLanguage()
    {
        return _Parameters.getString(ParameterNames.SpeechRecognitionLanguage);
    }
    public void setLanguage(String value)
    {
        _Parameters.set(ParameterNames.SpeechRecognitionLanguage, value);
    }

    /// <summary>
    /// The collection of parameters and their values defined for this <see cref="SpeechRecognizer"/>.
    /// </summary>
    public ParameterCollection<SpeechRecognizer> getParameters()
    {
        return _Parameters;
    }// { get; }
    private ParameterCollection<SpeechRecognizer> _Parameters;

    /// <summary>
    /// Starts speech recognition, and stops after the first utterance is recognized. The task returns the recognition text as result.
    /// </summary>
    /// <returns>A task representing the recognition operation. The task returns a value of <see cref="SpeechRecognitionResult"/> </returns>
    /// <example>
    /// The following example creates a speech recognizer, and then gets and prints the recognition result.
    /// <code>
    /// static void SpeechRecognizerSample()
    /// {
    ///   SpeechRecognizer reco = factory.CreateSpeechRecognizer("audioFileName");
    ///
    ///   // Starts recognition.
    ///   var result = await reco.RecognizeAsync();
    ///
    ///   Console.WriteLine("Speech Recognition: Recognition result: " + result);
    /// }
    /// </code>
    /// </example>
    public Task<SpeechRecognitionResult> recognizeAsync()
    {
        Task<SpeechRecognitionResult> t = new Task<SpeechRecognitionResult>(new TaskRunner() {
            SpeechRecognitionResult result;
            
            @Override
            public void run() {
                result = new SpeechRecognitionResult(recoImpl.recognize()); 
            }

            @Override
            public Object result() {
                return result;
            }});
        
        return t;
    }

    /// <summary>
    /// Starts speech recognition on a continous audio stream, until StopContinuousRecognitionAsync() is called.
    /// User must subscribe to events to receive recognition results.
    /// </summary>
    /// <returns>A task representing the asynchronous operation that starts the recognition.</returns>
    public Task<?> startContinuousRecognitionAsync()
    {
        Task<?> t = new Task(new TaskRunner() {

            @Override
            public void run() {
                recoImpl.startContinuousRecognition();
            }

            @Override
            public Object result() {
                return null;
            }});
        
        return t;
    }

    /// <summary>
    /// Stops continuous speech recognition.
    /// </summary>
    /// <returns>A task representing the asynchronous operation that stops the recognition.</returns>
    public Task<?> stopContinuousRecognitionAsync()
    {
        Task<?> t = new Task(new TaskRunner() {

            @Override
            public void run() {
                recoImpl.stopContinuousRecognition();
            }

            @Override
            public Object result() {
                return null;
            }});
        
        return t;
    }

    /// <summary>
    /// Starts speech recognition on a continous audio stream with keyword spotting, until StopKeywordRecognitionAsync() is called.
    /// User must subscribe to events to receive recognition results.
    /// </summary>
    /// <returns>A task representing the asynchronous operation that starts the recognition.</returns>
    public Task<?> startKeywordRecognitionAsync(String keyword)
    {
        Task<?> t = new Task(new TaskRunner() {

            @Override
            public void run() {
                recoImpl.startKeywordRecognition(keyword);
            }

            @Override
            public Object result() {
                return null;
            }});
        
        return t;
    }

    /// <summary>
    /// Stops continuous speech recognition.
    /// </summary>
    /// <returns>A task representing the asynchronous operation that stops the recognition.</returns>
    public Task<?> stopKeywordRecognitionAsync()
    {
        Task<?> t = new Task(new TaskRunner() {

            @Override
            public void run() {
                recoImpl.stopKeywordRecognition();
            }

            @Override
            public Object result() {
                return null;
            }});
        
        return t;
    }
    
    @Override
    protected void dispose(boolean disposing) throws IOException
    {
        if (disposed)
        {
            return;
        }

        if (disposing)
        {
            getRecoImpl().getIntermediateResult().removeEventListener(intermediateResultHandler);
            getRecoImpl().getFinalResult().removeEventListener(finalResultHandler);
            getRecoImpl().getNoMatch().removeEventListener(errorHandler);
            getRecoImpl().getCanceled().removeEventListener(errorHandler);
            getRecoImpl().getSessionStarted().removeEventListener(sessionStartedHandler);
            getRecoImpl().getSessionStopped().removeEventListener(sessionStoppedHandler);
            getRecoImpl().getSpeechStartDetected().removeEventListener(speechStartDetectedHandler);
            getRecoImpl().getSpeechEndDetected().removeEventListener(speechEndDetectedHandler);

            intermediateResultHandler.delete();
            finalResultHandler.delete();
            errorHandler.delete();
            getRecoImpl().delete();
            _Parameters.close();
            disposed = true;
            super.dispose(disposing);
        }
    }

    public com.microsoft.cognitiveservices.speech.internal.SpeechRecognizer getRecoImpl() {
        return recoImpl;
    }

    private com.microsoft.cognitiveservices.speech.internal.SpeechRecognizer recoImpl;
    private ResultHandlerImpl intermediateResultHandler;
    private ResultHandlerImpl finalResultHandler;
    private ErrorHandlerImpl errorHandler;
    private boolean disposed = false;

    // Defines an internal class to raise a C# event for intermediate/final result when a corresponding callback is invoked by the native layer.
    private class ResultHandlerImpl extends com.microsoft.cognitiveservices.speech.internal.SpeechRecognitionEventListener
    {
        ResultHandlerImpl(SpeechRecognizer recognizer, boolean isFinalResultHandler)
        {
            this.recognizer = recognizer;
            this.isFinalResultHandler = isFinalResultHandler;
        }

        @Override
        public void execute(com.microsoft.cognitiveservices.speech.internal.SpeechRecognitionEventArgs eventArgs)
        {
            if (recognizer.disposed)
            {
                return;
            }

            SpeechRecognitionResultEventArgs resultEventArg = new SpeechRecognitionResultEventArgs(eventArgs);
            EventHandlerImpl<SpeechRecognitionResultEventArgs> handler = isFinalResultHandler ? recognizer.FinalResultReceived : recognizer.IntermediateResultReceived;
            if (handler != null)
            {
                handler.fireEvent(this.recognizer, resultEventArg);
            }
        }

        private SpeechRecognizer recognizer;
        private boolean isFinalResultHandler;
    }

    // Defines an internal class to raise a C# event for error during recognition when a corresponding callback is invoked by the native layer.
    private class ErrorHandlerImpl extends com.microsoft.cognitiveservices.speech.internal.SpeechRecognitionEventListener
    {
        ErrorHandlerImpl(SpeechRecognizer recognizer)
        {
            this.recognizer = recognizer;
        }

        @Override
        public void execute(com.microsoft.cognitiveservices.speech.internal.SpeechRecognitionEventArgs eventArgs)
        {
            if (recognizer.disposed)
            {
                return;
            }

            RecognitionErrorEventArgs resultEventArg = new RecognitionErrorEventArgs(eventArgs.getSessionId(), eventArgs.getResult().getReason());
            EventHandlerImpl<RecognitionErrorEventArgs> handler = this.recognizer.RecognitionErrorRaised;

            if (handler != null)
            {
                handler.fireEvent(this.recognizer, resultEventArg);
            }
        }

        private SpeechRecognizer recognizer;
    }
}
