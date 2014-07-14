/***********************************************************************************************************************
 *
 * Copyright (C) 2010-2014 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.streaming.api;

import java.util.LinkedList;
import java.util.List;

import eu.stratosphere.nephele.io.ChannelSelector;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractInputTask;
import eu.stratosphere.streaming.api.invokable.DefaultSourceInvokable;
import eu.stratosphere.streaming.api.invokable.UserSourceInvokable;
import eu.stratosphere.streaming.partitioner.DefaultPartitioner;
import eu.stratosphere.streaming.test.RandIS;
import eu.stratosphere.types.Record;

public class StreamSource extends AbstractInputTask<RandIS> {

  private List<RecordWriter<Record>> outputs;
  private List<ChannelSelector<Record>> partitioners;
  private UserSourceInvokable userFunction;

  private int numberOfOutputs;

  public StreamSource() {
    // TODO: Make configuration file visible and call setClassInputs() here
    outputs = new LinkedList<RecordWriter<Record>>();
    partitioners = new LinkedList<ChannelSelector<Record>>();
    userFunction = null;
    numberOfOutputs = 0;

  }

  @Override
  public RandIS[] computeInputSplits(int requestedMinNumber) throws Exception {
    return null;
  }

  @Override
  public Class<RandIS> getInputSplitType() {
    return null;
  }

  private void setConfigInputs() {

    numberOfOutputs = getTaskConfiguration().getInteger("numberOfOutputs", 0);
    Class<? extends ChannelSelector<Record>> partitioner;

    for (int i = 1; i <= numberOfOutputs; i++) {
      partitioner = getTaskConfiguration().getClass("partitioner_" + i,
          DefaultPartitioner.class, ChannelSelector.class);

      try {
        partitioners.add(partitioner.newInstance());
        // System.out.println("partitioner added");
      } catch (Exception e) {
        // System.out.println("partitioner error" + " " + "partitioner_"
        // + i);
      }
    }

    for (ChannelSelector<Record> outputPartitioner : partitioners) {
      outputs.add(new RecordWriter<Record>(this, Record.class,
          outputPartitioner));
    }

    Class<? extends UserSourceInvokable> userFunctionClass;
    userFunctionClass = getTaskConfiguration().getClass("userfunction",
        DefaultSourceInvokable.class, UserSourceInvokable.class);

    try {
      userFunction = userFunctionClass.newInstance();
      userFunction.declareOutputs(outputs);
    } catch (Exception e) {

    }

  }

  @Override
  public void registerInputOutput() {
    setConfigInputs();

  }

  @Override
  public void invoke() throws Exception {

    userFunction.invoke();

  }

}